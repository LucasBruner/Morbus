package br.com.morbus.queueservice.domain.enums;

public enum EPriorityGroup {

    IDOSO(1),
    GESTANTE(2),
    DEFICIENTE(3),
    LACTANTE(4),
    OBESO(5),
    GERAL(6);

    private final int numericPriority;

    EPriorityGroup(int numericPriority) {
        this.numericPriority = numericPriority;
    }

    public int getNumericPriority() {
        return numericPriority;
    }
}
