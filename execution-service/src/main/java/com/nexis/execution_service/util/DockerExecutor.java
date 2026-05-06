package com.nexis.execution_service.util;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.command.CreateContainerCmdImpl;
import com.nexis.execution_service.config.type.CodeLanguage;
import com.nexis.execution_service.entity.ExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.UUID;

    @Component
    @Slf4j
    @RequiredArgsConstructor
    public class DockerExecutor {

        private final DockerClient dockerClient;

        public ExecutionResult execute(UUID jobId, CodeLanguage codeLanguage, String code) {

            ContainerSetup setup = getContainerSetup(codeLanguage, code);

            /*By default, a Docker container is greedy. If you start a Python container and don't give it rules,
             it has access to 100% of your Ubuntu machine's RAM, 100% of your CPU,
              and full access to your internet connection.
                HostConfig is the set of physical, OS-level rules you force onto the container.*/

            HostConfig hostConfig = HostConfig.newHostConfig()
                    .withMemory(134217728L) // 128 MB in bytes (128 * 1024 * 1024)
                    .withNetworkMode("none"); // Completely disables internet access inside the container

            // 4. Build and send the Create Command to the Docker Daemon (a Builder pattern)
            CreateContainerResponse container = dockerClient.createContainerCmd(setup.image())
                    .withName("nexis-job-" + jobId.toString())
                    .withCmd(setup.command())
                    .withHostConfig(hostConfig)
                    .exec(); // .exec() physically sends the HTTP POST request to the Unix Socket

            String containerId = container.getId();
            System.out.println("Created container: " + containerId);

            return null;
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
