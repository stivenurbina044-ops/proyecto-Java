package com.littlehorizons.erp.model;

public enum BorrowerType {
    STUDENT("Estudiante", "prestamos_estudiante", "estudiantes", "id_estudiante", "fecha_ingreso"),
    TEACHER("Profesor", "prestamos_profesor", "profesores", "id_profesor", "fecha_contratacion");

    private final String label;
    private final String loanTable;
    private final String borrowerTable;
    private final String borrowerColumn;
    private final String startDateColumn;

    BorrowerType(String label, String loanTable, String borrowerTable,
                 String borrowerColumn, String startDateColumn) {
        this.label = label;
        this.loanTable = loanTable;
        this.borrowerTable = borrowerTable;
        this.borrowerColumn = borrowerColumn;
        this.startDateColumn = startDateColumn;
    }

    public String label() { return label; }
    public String loanTable() { return loanTable; }
    public String borrowerTable() { return borrowerTable; }
    public String borrowerColumn() { return borrowerColumn; }
    public String startDateColumn() { return startDateColumn; }
}
