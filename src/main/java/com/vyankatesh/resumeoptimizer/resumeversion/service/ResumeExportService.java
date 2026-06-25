package com.vyankatesh.resumeoptimizer.resumeversion.service;

import com.vyankatesh.resumeoptimizer.resumeversion.dto.ResumeVersionResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class ResumeExportService {

    private final ResumeVersionService resumeVersionService;

    public ResumeExportService(ResumeVersionService resumeVersionService) {
        this.resumeVersionService = resumeVersionService;
    }

    public byte[] exportToPdf(Long versionId) {
        ResumeVersionResponse resume = resumeVersionService.getVersionById(versionId);

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDPage page = new PDPage();
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
            contentStream.newLineAtOffset(50, 750);
            contentStream.showText(resume.getVersionName());
            contentStream.endText();

            writePdfText(contentStream, "Template: " + resume.getTemplateName(), 50, 720, 12);
            writePdfText(contentStream, "ATS Score: " + resume.getAtsScore(), 50, 700, 12);

            writePdfText(contentStream, "Professional Summary", 50, 670, 14);
            writePdfText(contentStream, safe(resume.getProfessionalSummary()), 50, 650, 11);

            writePdfText(contentStream, "Skills", 50, 590, 14);
            writePdfText(contentStream, safe(resume.getSkills()), 50, 570, 11);

            writePdfText(contentStream, "Experience", 50, 520, 14);
            writePdfText(contentStream, safe(resume.getExperienceBullets()), 50, 500, 11);

            writePdfText(contentStream, "Projects", 50, 440, 14);
            writePdfText(contentStream, safe(resume.getProjectBullets()), 50, 420, 11);

            writePdfText(contentStream, "Education", 50, 360, 14);
            writePdfText(contentStream, safe(resume.getEducation()), 50, 340, 11);

            contentStream.close();

            document.save(outputStream);
            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to export resume as PDF", e);
        }
    }

    public byte[] exportToDocx(Long versionId) {
        ResumeVersionResponse resume = resumeVersionService.getVersionById(versionId);

        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            addHeading(document, resume.getVersionName(), 18);
            addParagraph(document, "Template: " + resume.getTemplateName());
            addParagraph(document, "ATS Score: " + resume.getAtsScore());

            addHeading(document, "Professional Summary", 14);
            addParagraph(document, resume.getProfessionalSummary());

            addHeading(document, "Skills", 14);
            addParagraph(document, resume.getSkills());

            addHeading(document, "Experience", 14);
            addParagraph(document, resume.getExperienceBullets());

            addHeading(document, "Projects", 14);
            addParagraph(document, resume.getProjectBullets());

            addHeading(document, "Education", 14);
            addParagraph(document, resume.getEducation());

            document.write(outputStream);
            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to export resume as DOCX", e);
        }
    }

    private void writePdfText(
            PDPageContentStream contentStream,
            String text,
            float x,
            float y,
            int fontSize
    ) throws Exception {
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, fontSize);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(cleanText(text));
        contentStream.endText();
    }

    private void addHeading(XWPFDocument document, String text, int size) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setBold(true);
        run.setFontSize(size);
        run.setText(safe(text));
    }

    private void addParagraph(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setFontSize(11);
        run.setText(safe(text));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String cleanText(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\n", " ")
                .replace("\r", " ")
                .replace("•", "-");
    }
}