package br.com.morbus.queueservice.domain.exception;

    public class PatientInactivatedException extends RuntimeException {
        public PatientInactivatedException(String e) {
            super(e);
        }
    }
