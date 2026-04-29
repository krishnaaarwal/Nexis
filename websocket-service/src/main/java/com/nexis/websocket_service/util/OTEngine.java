package com.nexis.websocket_service.util;

import com.nexis.websocket_service.config.type.OperationType;
import com.nexis.websocket_service.payload.CodeOperation;

public class OTEngine {

    public static CodeOperation transform(CodeOperation incoming, CodeOperation historical) {

        if (historical.getOperationType() == OperationType.RETAIN) {
            return incoming;
        }

        if(historical.getOperationType().equals(OperationType.INSERT)){

            //RULE 1: INSERT VS INSERT
            if(incoming.getOperationType().equals(OperationType.INSERT)){
                return insert_insert(incoming,historical);
            } //RULE 2: INSERT VS DELETE
            else {
                return insert_delete(incoming,historical);
            }

        }
        else {
            //RULE 3: DELETE VS INSERT
            if(incoming.getOperationType().equals(OperationType.INSERT)){
                return delete_insert(incoming,historical);
            } //RULE 4: DELETE VS DELETE
            else {
                return delete_delete(incoming,historical);
            }
        }

    }

    private static CodeOperation delete_delete(CodeOperation incoming, CodeOperation historical) {

        /*
        The Scenario:

    Historical: Deletes position = 2, length = 5 (Deletes indices 2, 3, 4, 5, 6).

    Incoming: Deletes position = 4, length = 2 (Deletes indices 4, 5).

    The historical operation already deleted the text the incoming operation is trying to delete! The incoming operation is swinging at a ghost.
         */

        if (historical.getPosition() <= incoming.getPosition()) {

            int histEnd = historical.getPosition() + historical.getLength();
            int incEnd = incoming.getPosition() + incoming.getLength();

            if (histEnd >= incEnd) {
                // The historical deletion completely swallowed the incoming deletion. Neutralize it.
                incoming.setOperationType(OperationType.RETAIN);
                incoming.setLength(0);
            } else {
                // Partial overlap or no overlap, safely shift the starting position left.
                int newPosition = Math.max(historical.getPosition(), incoming.getPosition() - historical.getLength());
                incoming.setPosition(newPosition);
            }
        }
        else{
            //Nothing to do! (Unaffected)
        }
        return incoming;
    }

    private static CodeOperation delete_insert(CodeOperation incoming, CodeOperation historical) {

        if(historical.getPosition() < incoming.getPosition()){

            // If the deletion swallows the incoming cursor, clamp the cursor to the start of the deletion.
            int newPosition = Math.max(historical.getPosition(), incoming.getPosition() - historical.getLength());
            incoming.setPosition(newPosition);

            /*
            Logic:  Agar historical highlight index 2 to 8 and press backspace and income insert at 5
            Then historic.getpostion(2) < incoming.getPostion(5)
            calculates incoming.setPosition(5 - 6) = -1    -> IndexOutOfBoundException

            To fix this bug, we just put the incoming cursor at the start of deleting position
             */


        }else {
            //Nothing to do! (Unaffected)
        }
        return incoming;
    }

    private static CodeOperation insert_delete(CodeOperation incoming, CodeOperation historical) {
        if(historical.getPosition() <= incoming.getPosition()){

            int codeLength = historical.getCode().length();
            incoming.setPosition(incoming.getPosition()+codeLength);

        }else {
            //Nothing to do! (Unaffected)
        }
        return incoming;
    }

    private static CodeOperation insert_insert(CodeOperation incoming, CodeOperation historical){
        if(historical.getPosition() < incoming.getPosition()){

            int codeLength = historical.getCode().length();
            incoming.setPosition(incoming.getPosition() + codeLength);

        } else if (historical.getPosition() == incoming.getPosition()) { //TIEBREAKER
            //I give priority to historic first to insert first , since my userId is UUID i can't compare them to priority comparison
            int codeLength = historical.getCode().length();
            incoming.setPosition(incoming.getPosition() + codeLength);

        } else {
            //Nothing to do! (Unaffected)
        }
        return incoming;
    }

}
