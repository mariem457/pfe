package com.example.demo.dto;

public class QualifyPublicReportRequest {

    private String qualificationNote;
    private Long duplicateOfReportId;

    public String getQualificationNote() {
        return qualificationNote;
    }

    public void setQualificationNote(String qualificationNote) {
        this.qualificationNote = qualificationNote;
    }

    public Long getDuplicateOfReportId() {
        return duplicateOfReportId;
    }

    public void setDuplicateOfReportId(Long duplicateOfReportId) {
        this.duplicateOfReportId = duplicateOfReportId;
    }
}