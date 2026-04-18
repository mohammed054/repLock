package com.replock.app.domain.model

/**
 * Top-level session state machine (Phase 5.2).
 *
 *  STANDBY  ──[pose held 1.5 s]──►  READY
 *  READY    ──[first rep motion]──►  ACTIVE
 *  ACTIVE   ──[pose lost ≥ 2 s] ──►  SET_COMPLETE
 *  SET_COMPLETE  ──[3 s / tap] ──►  STANDBY
 */
enum class SessionState {
    /** Camera live, skeleton drawn, rep counter idle. UI: "GET INTO POSITION". */
    STANDBY,

    /** Valid pose held for 1.5 s. Audio beep + green flash. UI: "START!" */
    READY,

    /** Rep counting live. Elapsed timer running. */
    ACTIVE,

    /** Pose lost for ≥ 2 continuous seconds. Rep count frozen. Summary shown. */
    SET_COMPLETE
}
