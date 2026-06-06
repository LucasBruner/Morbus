package br.com.morbus.queueservice.domain.exception;

    public class PatientNotFoundException extends RuntimeException {
        public PatientNotFoundException(String e) {
            super(e);
        }
    }
