package org.eclipse.kura.core.linux.executor;

import org.eclipse.kura.executor.Signal;

public enum LinuxSignal implements Signal {

    SIGHUP {

        @Override
        public int getSignalNumber() {
            return 1;
        }
    },
    SIGINT {

        @Override
        public int getSignalNumber() {
            return 2;
        }
    },
    SIGQUIT {

        @Override
        public int getSignalNumber() {
            return 3;
        }
    },
    SIGILL {

        @Override
        public int getSignalNumber() {
            return 4;
        }
    },
    SIGTRAP {

        @Override
        public int getSignalNumber() {
            return 5;
        }
    },
    SIGABRT {

        @Override
        public int getSignalNumber() {
            return 6;
        }
    },
    SIGBUS {

        @Override
        public int getSignalNumber() {
            return 7;
        }
    },
    SIGFPE {

        @Override
        public int getSignalNumber() {
            return 8;
        }
    },
    SIGKILL {

        @Override
        public int getSignalNumber() {
            return 9;
        }
    },
    SIGUSR1 {

        @Override
        public int getSignalNumber() {
            return 10;
        }
    },
    SIGSEGV {

        @Override
        public int getSignalNumber() {
            return 11;
        }
    },
    SIGUSR2 {

        @Override
        public int getSignalNumber() {
            return 12;
        }
    },
    SIGPIPE {

        @Override
        public int getSignalNumber() {
            return 13;
        }
    },
    SIGALRM {

        @Override
        public int getSignalNumber() {
            return 14;
        }
    },
    SIGTERM {

        @Override
        public int getSignalNumber() {
            return 15;
        }
    },
    SIGSTKFLT {

        @Override
        public int getSignalNumber() {
            return 16;
        }
    },
    SIGCHLD {

        @Override
        public int getSignalNumber() {
            return 17;
        }
    },
    SIGCONT {

        @Override
        public int getSignalNumber() {
            return 18;
        }
    },
    SIGSTOP {

        @Override
        public int getSignalNumber() {
            return 19;
        }
    },
    SIGTSTP {

        @Override
        public int getSignalNumber() {
            return 20;
        }
    },
    SIGTTIN {

        @Override
        public int getSignalNumber() {
            return 21;
        }
    },
    SIGTTOU {

        @Override
        public int getSignalNumber() {
            return 22;
        }
    },
    SIGURG {

        @Override
        public int getSignalNumber() {
            return 23;
        }
    },
    SIGXCPU {

        @Override
        public int getSignalNumber() {
            return 24;
        }
    },
    SIGXFSZ {

        @Override
        public int getSignalNumber() {
            return 25;
        }
    },
    SIGVTALRM {

        @Override
        public int getSignalNumber() {
            return 26;
        }
    },
    SIGPROF {

        @Override
        public int getSignalNumber() {
            return 27;
        }
    },
    SIGWINCH {

        @Override
        public int getSignalNumber() {
            return 28;
        }
    },
    SIGIO {

        @Override
        public int getSignalNumber() {
            return 29;
        }
    },
    SIGPWR {

        @Override
        public int getSignalNumber() {
            return 30;
        }
    },
    SIGSYS {

        @Override
        public int getSignalNumber() {
            return 31;
        }
    },
    SIGRTMIN {

        @Override
        public int getSignalNumber() {
            return 34;
        }
    },
    SIGRTMIN_PLUS_1 {

        @Override
        public int getSignalNumber() {
            return 35;
        }
    },
    SIGRTMIN_PLUS_2 {

        @Override
        public int getSignalNumber() {
            return 36;
        }
    },
    SIGRTMIN_PLUS_3 {

        @Override
        public int getSignalNumber() {
            return 37;
        }
    },
    SIGRTMIN_PLUS_4 {

        @Override
        public int getSignalNumber() {
            return 38;
        }
    },
    SIGRTMIN_PLUS_5 {

        @Override
        public int getSignalNumber() {
            return 39;
        }
    },
    SIGRTMIN_PLUS_6 {

        @Override
        public int getSignalNumber() {
            return 40;
        }
    },
    SIGRTMIN_PLUS_7 {

        @Override
        public int getSignalNumber() {
            return 41;
        }
    },
    SIGRTMIN_PLUS_8 {

        @Override
        public int getSignalNumber() {
            return 42;
        }
    },
    SIGRTMIN_PLUS_9 {

        @Override
        public int getSignalNumber() {
            return 43;
        }
    },
    SIGRTMIN_PLUS_10 {

        @Override
        public int getSignalNumber() {
            return 44;
        }
    },
    SIGRTMIN_PLUS_11 {

        @Override
        public int getSignalNumber() {
            return 45;
        }
    },
    SIGRTMIN_PLUS_12 {

        @Override
        public int getSignalNumber() {
            return 46;
        }
    },
    SIGRTMIN_PLUS_13 {

        @Override
        public int getSignalNumber() {
            return 47;
        }
    },
    SIGRTMIN_PLUS_14 {

        @Override
        public int getSignalNumber() {
            return 48;
        }
    },
    SIGRTMIN_PLUS_15 {

        @Override
        public int getSignalNumber() {
            return 49;
        }
    },
    SIGRTMAX_MINUS_14 {

        @Override
        public int getSignalNumber() {
            return 50;
        }
    },
    SIGRTMAX_MINUS_13 {

        @Override
        public int getSignalNumber() {
            return 51;
        }
    },
    SIGRTMAX_MINUS_12 {

        @Override
        public int getSignalNumber() {
            return 52;
        }
    },
    SIGRTMAX_MINUS_11 {

        @Override
        public int getSignalNumber() {
            return 53;
        }
    },
    SIGRTMAX_MINUS_10 {

        @Override
        public int getSignalNumber() {
            return 54;
        }
    },
    SIGRTMAX_MINUS_9 {

        @Override
        public int getSignalNumber() {
            return 55;
        }
    },
    SIGRTMAX_MINUS_8 {

        @Override
        public int getSignalNumber() {
            return 56;
        }
    },
    SIGRTMAX_MINUS_7 {

        @Override
        public int getSignalNumber() {
            return 57;
        }
    },
    SIGRTMAX_MINUS_6 {

        @Override
        public int getSignalNumber() {
            return 58;
        }
    },
    SIGRTMAX_MINUS_5 {

        @Override
        public int getSignalNumber() {
            return 59;
        }
    },
    SIGRTMAX_MINUS_4 {

        @Override
        public int getSignalNumber() {
            return 60;
        }
    },
    SIGRTMAX_MINUS_3 {

        @Override
        public int getSignalNumber() {
            return 61;
        }
    },
    SIGRTMAX_MINUS_2 {

        @Override
        public int getSignalNumber() {
            return 62;
        }
    },
    SIGRTMAX_MINUS_1 {

        @Override
        public int getSignalNumber() {
            return 63;
        }
    },
    SIGRTMAX {

        @Override
        public int getSignalNumber() {
            return 64;
        }
    };
}
