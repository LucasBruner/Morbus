package br.com.morbus.queueservice.domain.enums;

public enum ERiskColor {

    VERMELHO(1),
    AMARELO(2),   
    VERDE(3),
    AZUL(4);

    private final int numericPriority;

    ERiskColor(int numericPriority) {
        this.numericPriority = numericPriority;
    }

    public int getNumericPriority() {
        return numericPriority;
    }
}
