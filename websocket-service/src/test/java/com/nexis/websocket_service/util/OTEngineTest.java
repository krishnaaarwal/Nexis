package com.nexis.websocket_service.util;

import com.nexis.websocket_service.config.type.OperationType;
import com.nexis.websocket_service.payload.CodeOperation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for OTEngine — The 4 Rules
 * No Spring context needed. Pure Java logic.
 * Run with: mvn test
 */
class OTEngineTest {

    // ─── Helper: build a quick CodeOperation ─────────────────────────────────
    private CodeOperation insert(int position, String code) {
        CodeOperation op = new CodeOperation();
        op.setUserId(UUID.randomUUID());
        op.setOperationType(OperationType.INSERT);
        op.setPosition(position);
        op.setCode(code);
        op.setVersion(1);
        return op;
    }

    private CodeOperation delete(int position, int length) {
        CodeOperation op = new CodeOperation();
        op.setUserId(UUID.randomUUID());
        op.setOperationType(OperationType.DELETE);
        op.setPosition(position);
        op.setLength(length);
        op.setVersion(1);
        return op;
    }

    private CodeOperation retain() {
        CodeOperation op = new CodeOperation();
        op.setOperationType(OperationType.RETAIN);
        op.setLength(0);
        return op;
    }

    // ─── RULE 1: INSERT vs INSERT ─────────────────────────────────────────────
    @Nested
    @DisplayName("Rule 1: INSERT vs INSERT")
    class InsertVsInsert {

        @Test
        @DisplayName("Historical BEFORE incoming → incoming shifts right")
        void historicalBeforeIncoming() {
            // Doc: "HELLO"
            // Historical: INSERT "X" at position 2 → "HEXLLO" (already happened)
            // Incoming:   INSERT "Y" at position 4 (written against "HELLO")
            // Expected:   incoming shifts to position 5 (2 < 4, shift by len("X") = 1)

            CodeOperation historical = insert(2, "X");
            CodeOperation incoming   = insert(4, "Y");

            CodeOperation result = OTEngine.transform(incoming, historical);

            assertEquals(5, result.getPosition(),
                    "Incoming should shift right by length of historical insert");
            assertEquals(OperationType.INSERT, result.getOperationType());
            assertEquals("Y", result.getCode());
        }

        @Test
        @DisplayName("Historical AFTER incoming → incoming unaffected")
        void historicalAfterIncoming() {
            // Historical: INSERT "X" at position 7 (after incoming)
            // Incoming:   INSERT "Y" at position 3
            // Expected:   incoming stays at position 3

            CodeOperation historical = insert(7, "X");
            CodeOperation incoming   = insert(3, "Y");

            CodeOperation result = OTEngine.transform(incoming, historical);

            assertEquals(3, result.getPosition(),
                    "Incoming should NOT shift when historical is after it");
        }

        @Test
        @DisplayName("Same position TIEBREAKER → historical wins, incoming shifts")
        void samePositionTiebreaker() {
            // Both try to insert at position 5
            // Historical wins (already applied) → incoming shifts right

            CodeOperation historical = insert(5, "AB");  // inserts 2 chars
            CodeOperation incoming   = insert(5, "Y");

            CodeOperation result = OTEngine.transform(incoming, historical);

            assertEquals(7, result.getPosition(),
                    "Incoming should shift by len('AB')=2 when tiebreaker gives historical priority");
        }

        @Test
        @DisplayName("Historical multi-char insert shifts incoming by full length")
        void multiCharInsert() {
            // Historical: INSERT "HELLO" (5 chars) at position 0
            // Incoming:   INSERT "X" at position 3
            // Expected:   incoming shifts to 3 + 5 = 8

            CodeOperation historical = insert(0, "HELLO");
            CodeOperation incoming   = insert(3, "X");

            CodeOperation result = OTEngine.transform(incoming, historical);

            assertEquals(8, result.getPosition(),
                    "Should shift by full length of multi-char historical insert");
        }
    }

    // ─── RULE 2: INSERT vs DELETE (incoming=INSERT, historical=DELETE) ────────
    @Nested
    @DisplayName("Rule 2: INSERT vs DELETE")
    class InsertVsDelete {

        @Test
        @DisplayName("Historical delete BEFORE incoming insert → incoming shifts left")
        void historicalDeleteBeforeIncoming() {
            // Doc: "SPRING BOOT"
            // Historical: DELETE 3 chars at position 0 → "ING BOOT" (already happened)
            // Incoming:   INSERT "X" at position 7 (written against "SPRING BOOT")
            // Expected:   incoming shifts to 7 - 3 = 4

            CodeOperation historical = delete(0, 3);
            CodeOperation incoming   = insert(7, "X");

            CodeOperation result = OTEngine.transform(incoming, historical);

            assertEquals(4, result.getPosition(),
                    "Incoming insert should shift left when historical deleted chars before it");
        }

        @Test
        @DisplayName("Historical delete AT incoming position → incoming clamped")
        void historicalDeleteAtIncomingPosition() {
            // Historical: DELETE 5 chars at position 3
            // Incoming:   INSERT "X" at position 5 (inside the deleted range)
            // Expected:   incoming clamped to position 3 (start of deletion)

            CodeOperation historical = delete(3, 5);
            CodeOperation incoming   = insert(5, "X");

            CodeOperation result = OTEngine.transform(incoming, historical);

            assertEquals(3, result.getPosition(),
                    "Incoming insert inside deleted range should clamp to deletion start");
        }

        @Test
        @DisplayName("Historical delete AFTER incoming insert → unaffected")
        void historicalDeleteAfterIncoming() {
            // Historical: DELETE at position 10
            // Incoming:   INSERT at position 3
            // Expected:   incoming stays at 3

            CodeOperation historical = delete(10, 2);
            CodeOperation incoming   = insert(3, "X");

            CodeOperation result = OTEngine.transform(incoming, historical);

            assertEquals(3, result.getPosition(),
                    "Incoming should not shift when historical delete is after it");
        }
    }

    // ─── RULE 3: DELETE vs INSERT (incoming=DELETE, historical=INSERT) ────────
    @Nested
    @DisplayName("Rule 3: DELETE vs INSERT")
    class DeleteVsInsert {

        @Test
        @DisplayName("Historical insert BEFORE incoming delete → incoming shifts right")
        void historicalInsertBeforeIncoming() {
            // Historical: INSERT "XX" at position 2 (already happened)
            // Incoming:   DELETE at position 5
            // Expected:   incoming shifts to 5 + 2 = 7

            CodeOperation historical = insert(2, "XX");
            CodeOperation incoming   = delete(5, 1);

            CodeOperation result = OTEngine.transform(incoming, historical);

            assertEquals(7, result.getPosition(),
                    "Incoming delete should shift right by length of historical insert before it");
        }

        @Test
        @DisplayName("Historical insert AFTER incoming delete → unaffected")
        void historicalInsertAfterIncoming() {
            // Historical: INSERT at position 8
            // Incoming:   DELETE at position 3
            // Expected:   incoming stays at 3

            CodeOperation historical = insert(8, "Z");
            CodeOperation incoming   = delete(3, 1);

            CodeOperation result = OTEngine.transform(incoming, historical);

            assertEquals(3, result.getPosition(),
                    "Incoming delete should not shift when historical insert is after it");
        }
    }

    // ─── RULE 4: DELETE vs DELETE ─────────────────────────────────────────────
    @Nested
    @DisplayName("Rule 4: DELETE vs DELETE")
    class DeleteVsDelete {

        @Test
        @DisplayName("Historical DELETE completely before incoming → incoming shifts left")
        void historicalBeforeIncoming() {
            // Historical: DELETE 3 chars at position 1
            // Incoming:   DELETE at position 6
            // Expected:   incoming shifts to 6 - 3 = 3

            CodeOperation historical = delete(1, 3);
            CodeOperation incoming   = delete(6, 2);

            CodeOperation result = OTEngine.transform(incoming, historical);

            assertEquals(3, result.getPosition(),
                    "Incoming delete should shift left by historical delete length");
        }

        @Test
        @DisplayName("Historical DELETE completely swallows incoming → RETAIN (no-op)")
        void historicalSwallowsIncoming() {
            // Historical: DELETE 5 chars at position 2 (covers indices 2,3,4,5,6)
            // Incoming:   DELETE 2 chars at position 4 (covers indices 4,5 — already gone!)
            // Expected:   incoming becomes RETAIN (ghost delete)

            CodeOperation historical = delete(2, 5);
            CodeOperation incoming   = delete(4, 2);

            CodeOperation result = OTEngine.transform(incoming, historical);

            assertEquals(OperationType.RETAIN, result.getOperationType(),
                    "Incoming delete targeting already-deleted chars should become RETAIN");
            assertEquals(0, result.getLength(),
                    "RETAIN length should be 0 for neutralized delete");
        }

        @Test
        @DisplayName("Same position delete → incoming becomes RETAIN")
        void samePositionDelete() {
            // Both delete at same position — historical already did it
            // Incoming is a ghost

            CodeOperation historical = delete(3, 1);
            CodeOperation incoming   = delete(3, 1);

            CodeOperation result = OTEngine.transform(incoming, historical);

            assertEquals(OperationType.RETAIN, result.getOperationType(),
                    "Deleting already-deleted position should become RETAIN");
        }

        @Test
        @DisplayName("Historical DELETE after incoming → unaffected")
        void historicalAfterIncoming() {
            // Historical: DELETE at position 8
            // Incoming:   DELETE at position 2
            // Expected:   incoming stays at 2

            CodeOperation historical = delete(8, 2);
            CodeOperation incoming   = delete(2, 1);

            CodeOperation result = OTEngine.transform(incoming, historical);

            assertEquals(2, result.getPosition(),
                    "Incoming should not shift when historical delete is after it");
            assertEquals(OperationType.DELETE, result.getOperationType());
        }
    }

    // ─── RETAIN passthrough ───────────────────────────────────────────────────
    @Nested
    @DisplayName("RETAIN passthrough")
    class RetainPassthrough {

        @Test
        @DisplayName("Historical RETAIN → incoming always unaffected")
        void historicalRetain() {
            CodeOperation historical = retain();
            CodeOperation incoming   = insert(5, "X");

            CodeOperation result = OTEngine.transform(incoming, historical);

            assertEquals(5, result.getPosition(), "RETAIN historical should never affect incoming");
            assertEquals(OperationType.INSERT, result.getOperationType());
        }
    }

    // ─── REAL WORLD SCENARIO ─────────────────────────────────────────────────
    @Nested
    @DisplayName("Real World: Multiple concurrent operations")
    class RealWorldScenarios {

        @Test
        @DisplayName("Google Docs scenario: 3 users edit SPRING BOOT simultaneously")
        void threeUsersConcurrentEdit() {
            // Document: "SPRING BOOT" (11 chars, 0-indexed)
            // User A (applied first):  INSERT "!" at 11 → "SPRING BOOT!"
            // User B (applied second): INSERT "S" at 0  — needs transform
            // User C (applied third):  DELETE "O" at 9  — needs transform through A and B

            // Transform User B's op against User A:
            CodeOperation userA = insert(11, "!");
            CodeOperation userB = insert(0, "S");

            CodeOperation transformedB = OTEngine.transform(userB, userA);
            // A inserted at 11, B inserts at 0 → 11 > 0 → B unaffected
            assertEquals(0, transformedB.getPosition(),
                    "B inserts before A's position — unaffected");

            // Transform User C's op against User A:
            CodeOperation userC = delete(9, 1);  // delete "O"
            CodeOperation transformedC_afterA = OTEngine.transform(userC, userA);
            // A inserted at 11, C deletes at 9 → 11 > 9 → C unaffected
            assertEquals(9, transformedC_afterA.getPosition(),
                    "C's delete position unaffected by A's insert after it");

            // Transform User C's op (already transformed against A) against B:
            // B inserted at 0, C deletes at 9 → B before C → C shifts right by 1
            CodeOperation transformedC_afterB = OTEngine.transform(transformedC_afterA, transformedB);
            assertEquals(10, transformedC_afterB.getPosition(),
                    "C's position should shift right by 1 due to B's insert at 0");
        }

        @Test
        @DisplayName("Negative position guard: delete before insert doesn't go negative")
        void negativePositionGuard() {
            // Historical: DELETE 6 chars at position 2
            // Incoming:   INSERT "X" at position 5 (inside deleted range)
            // Without Math.max → 5 - 6 = -1 → crash!
            // With Math.max    → max(2, -1) = 2 → safe

            CodeOperation historical = delete(2, 6);
            CodeOperation incoming   = insert(5, "X");

            CodeOperation result = OTEngine.transform(incoming, historical);

            assertTrue(result.getPosition() >= 0,
                    "Position should NEVER go negative — Math.max guard must work");
            assertEquals(2, result.getPosition(),
                    "Should clamp to deletion start position");
        }
    }
}
