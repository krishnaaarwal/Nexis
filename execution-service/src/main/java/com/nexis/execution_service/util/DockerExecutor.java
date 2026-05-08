package com.nexis.execution_service.util;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.StreamType;
import com.nexis.execution_service.config.type.CodeLanguage;
import com.nexis.execution_service.config.type.StatusType;
import com.nexis.execution_service.entity.ExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
    @Slf4j
    @RequiredArgsConstructor
    public class DockerExecutor {

        private final DockerClient dockerClient;

        public ExecutionResult execute(UUID jobId, UUID userId , UUID workspaceId,CodeLanguage codeLanguage, String code) {

            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            ContainerSetup setup = getContainerSetup(codeLanguage, code);

            /*By default, a Docker container is greedy. If you start a Python container and don't give it rules,
             it has access to 100% of your Ubuntu machine's RAM, 100% of your CPU,
              and full access to your internet connection.
                HostConfig is the set of physical, OS-level rules you force onto the container.*/

            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withMemory(134217728L) // 128 MB in bytes (128 * 1024 * 1024)
                    .withNetworkMode("none") // Completely disables internet access inside the container
                      .withCpuPeriod(100000L) // Set CPU cycle period
                    .withCpuQuota(50000L)   // Limit to 50,000 out of 100,000 (Exactly 50% CPU)
                    .withReadonlyRootfs(true) // Freezes the file system. They cannot install packages or create files anywhere except /tmp
                     .withBinds(Bind.parse("/tmp:/tmp")); // Give them a temporary scratchpad if they need to write files


        // Build and send the Create Command to the Docker Daemon (a Builder pattern)
            CreateContainerResponse container = dockerClient.createContainerCmd(setup.image())
                    .withName("nexis-job-" + jobId.toString())
                    .withCmd(setup.command())
                    .withHostConfig(hostConfig)
                    .exec(); // .exec() physically sends the HTTP POST request to the Unix Socket

            String containerId = container.getId();
            System.out.println("Created container: " + containerId);

            // Step 2 - Start the container
            dockerClient.startContainerCmd(containerId).exec();

            // Step 3 - Read the output
            try{
                dockerClient.logContainerCmd(containerId)
                        .withStdOut(true) //I want std output
                        .withStdErr(true) //I want std error
                        .withFollowStream(true) // Keep listening until the container stops
                        .exec(new ResultCallback.Adapter<>(){

                            // A Frame consists of two things:
                            //
                            //    The Header (Type): A tiny label that says, "Hey, this package is STDOUT" or "Hey, this package is STDERR".
                            //
                            //    The Payload: The actual byte array of the text (e.g., the word "Hello").


                            /*onNext(Frame frame):
                            The Workhorse. This fires every single time a piece of text comes out of the container.
                            If the container prints 100 lines, this method fires 100 times. Inside here,
                            you write the if/else logic to append the payload to your stdout or stderr StringBuilders.*/
                            @Override
                            public void onNext(Frame frame) {
                                if(frame.getStreamType() == StreamType.STDOUT){
                                    stdout.append(new String(frame.getPayload()));
                                } else if (frame.getStreamType() == StreamType.STDERR) {
                                    stderr.append(new String(frame.getPayload()));
                                }
                            }
                        })
                        .awaitCompletion(30, TimeUnit.SECONDS);  // It tells your Java thread:
                                                                        // "Stop right here. Do not execute the next line of code until the onComplete method fires.
                                                                        // ... OR until 30 seconds pass."

                /*If a user writes a while(true) loop in their workspace, the container will run forever.
                Without this timeout, your Java thread gets permanently blocked.
                With the timeout, after 30 seconds, Java forcefully throws an InterruptedException,
                 escapes the roadblock, and you can fail the job gracefully.*/
            } catch (Exception e) {
                stderr.append("\nExecution timed out after 30 seconds.");
            }

            // Step 4 - Destroy the container
            dockerClient.removeContainerCmd(containerId).withForce(true).exec();

            StatusType finalStatus = StatusType.COMPLETED;
            if (stderr.length() > 0) {
                finalStatus = StatusType.FAILED;
            }
            return new ExecutionResult(jobId,userId,workspaceId,finalStatus,stdout.toString(),stderr.toString());
        }

        private record ContainerSetup(String image, String[] command) {}

        private ContainerSetup getContainerSetup(CodeLanguage language, String code) {
            return switch (language) {
                case PYTHON -> new ContainerSetup(
                        "python:3.11-slim",
                        new String[]{"python", "-c", code}
                );
                case JAVASCRIPT -> new ContainerSetup(
                        "node:18-alpine",
                        new String[]{"node", "-e", code}
                );
                case JAVA -> new ContainerSetup(
                        "openjdk:17-alpine",
                        // For Java, we write the code to a file then run it
                        new String[]{"sh", "-c", "echo '" + code.replace("'", "'\\''") + "' > Main.java && java Main.java"}
                );
                case CPP -> new ContainerSetup(
                        "gcc:13",
                        new String[]{"sh", "-c", "echo '" + code.replace("'", "'\\''") + "' > main.cpp && g++ main.cpp -o main && ./main"}
                );
                case DART -> new ContainerSetup(
                        "dart:stable",
                        new String[]{"sh", "-c", "echo '" + code.replace("'", "'\\''") + "' > main.dart && dart run main.dart"}
                );
            };
    }
}
