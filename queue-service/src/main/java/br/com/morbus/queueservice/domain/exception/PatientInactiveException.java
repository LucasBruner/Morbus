package br.com.morbus.queueservice.domain.exception;

    public class PatientInactiveException extends RuntimeException {
        public PatientInactiveException(String e) {
            super(e);
        }
    }
