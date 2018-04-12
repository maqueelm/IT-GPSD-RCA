package com.gps.util;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.ParseException;
import com.gps.dao.ContractDao;
import com.gps.vo.*;
import jxl.*;
import jxl.read.biff.BiffException;
import jxl.write.*;
import jxl.write.biff.RowsExceededException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReportGeneratorUtil {
    private static Logger log = Logger.getLogger(ReportGeneratorUtil.class);
    //ROW and COL define the range to look for the first $vars in the template
    final static int ROW_LIMIT = 20;
    final static int COL_LIMIT = 5;
    final static int EXCEL_CELL_LIMIT = 32767;
    ContractDao contractDao;

    protected Hashtable<String, Integer> sheetCounters;
    protected String templateAccountFilter;
    protected ArrayList<PageTemplate> templateData;
    protected byte[] finishedReport;
    protected int wildKeyIndex;
    protected ArrayList<String> wildKeys;
    protected Hashtable<String, Integer> wildCardCounts;
    private List<String> slaTowerList;

    //the template for the report
    protected Workbook template;
    protected byte[] templateBytes;
    protected WritableWorkbook templateWriteable;
    protected WritableWorkbook report;
    protected ByteArrayOutputStream baos;
    protected Hashtable<String, WildCard> wildcards;

    protected int actionRow;
    protected int incidentRow;

    //Hashtable caches for the reporting sheets
    Hashtable<Integer, Sheet> templateSheets;
    Hashtable<Integer, WritableSheet> reportSheets;
    Hashtable<Integer, WritableSheet> writableTemplateSheets;

    //Hashtable for report data
    Hashtable<String, String> hashtable;
    protected ArrayList<Hashtable<String, String[]>> actionResponses;
    private boolean useTemplateFilter;
    private Interpreter bsh;
    private boolean includeGreen;
    private boolean timeStampPrinted;

    public ReportGeneratorUtil() {
        this.templateData = new ArrayList<PageTemplate>();
        this.wildKeys = new ArrayList<String>();
        this.bsh = new Interpreter();
        try {
            bsh.set("f", this);
        } catch (EvalError e) {
            log.error(com.gps.util.StringUtils.getStackTraceAsString(e));
        }
        this.useTemplateFilter = false;
        this.includeGreen = false;
        this.wildcards = new Hashtable<String, WildCard>();
        this.templateAccountFilter = null;
        this.report = null;
        this.templateWriteable = null;
        this.baos = null;
        this.finishedReport = null;
    }

    private List<String> getSlaTowerList() {
        return slaTowerList;
    }

    //******************************************************************************
    //** Template map creation
    //******************************************************************************


    public byte[] generateProfileReport(List<Contract> profileContracts, String path) {
        log.debug("generateProfileReport()..........");
        try {
            String templateName = path + "profile.xls";
            log.debug("reading xls template: " + templateName);
            File xlsTemplate = new File(templateName);
            log.debug("creating workbook.....");
            template = Workbook.getWorkbook(xlsTemplate);
            log.debug("Filling properties in a hashtable.......");

            log.debug("Prepare excel workbooks.......");
            prepareExcelWorkbooks();
            createTemplateMappings("profile");
            //writeReportForProfile(profileReports);
            writeReportForProfile(profileContracts);
            //removeEmptyRows();
            writeReportToBytes();

        } catch (BiffException e) {
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        log.debug("end generateAnalysisReport()");
        return finishedReport;
    }


    public byte[] generateAnalysisReport(List<Contract> contractList, String path) {
        log.debug("generateAnalysisReport()..........");
        try {
            String templateName = path + "Analysis.xls";
            log.debug("reading xls template: " + templateName);
            File xlsTemplate = new File(templateName);
            log.debug("creating workbook.....");
            template = Workbook.getWorkbook(xlsTemplate);
            log.debug("Filling properties in a hashtable.......");

            log.debug("Prepare excel workbooks.......");
            prepareExcelWorkbooks();
            createTemplateMappings("analysis");
            writeReportForAnalysis(contractList);
            writeReportToBytes();

        } catch (BiffException e) {
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        log.debug("end generateAnalysisReport()");
        return finishedReport;
    }

    /////////////////////////////////////////helper methods.///////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Iterates through the template file and creates a list of all the cell
     * mappings (variables, formulas, etc.)
     *
     * @param caller
     */
    private void createTemplateMappings(String caller) {
        if (template == null) {
            return;
        }
        //if no ^tableStart^ is found, we look for ^rowEnd^ and use that to determine
        //the row that will be copied
        //$colorMaps$, etc. will be outside the ^tableEnd^ but before a ^rowEnd^ col (in the row only case)
        for (int sheetCount = 0; sheetCount < template.getNumberOfSheets(); sheetCount++) {
            Sheet currSheet = template.getSheet(sheetCount);
            PageTemplate pt = new PageTemplate();

            int rowStart = -1;
            Cell tableStart = findTableStart(currSheet);
            Cell rowEnd = null;
            if (tableStart == null) {
                rowEnd = findRowEnd(currSheet);
                if (rowEnd != null) {
                    rowStart = rowEnd.getRow();
                }
                if (rowStart == -1) {
                    templateData.add(pt); //add a blank page template
                    continue; //skip this iteration
                }
            } else {//only if sheet has ^tableStart^
                rowStart = tableStart.getRow() + 1;
                if (rowStart > 0) {
                    pt.setStartCell(tableStart.getRow() + 1, tableStart.getColumn());
                    //Find wildcard tag in the same row as ^tableStart^
                    Cell[] tableStartRow = currSheet.getRow(tableStart.getRow());
                    for (int j = 0; j < tableStartRow.length; j++) {
                        Cell tsrCell = tableStartRow[j];
                        String contents = tsrCell.getContents();
                        try {
                            if (contents.toLowerCase().startsWith("^wildcard^")) {
                                handleWildCard(tsrCell, currSheet);
                            }
                        } catch (Exception e) {
                            log.error("Template Error (sheet = " + currSheet.getName() + " r=" + tsrCell.getRow() + " c=" + tsrCell.getColumn() + ") : " + contents);
                            log.error(e.getMessage());
                        }
                    }
                } else {
                    templateData.add(pt); //add a blank page template
                    continue; //skip this iteration
                }
            }

            //now we loop until a ^rowEnd^ or a ^tableEnd^ or run out of rows
            boolean tableEnd = false;
            for (int i = rowStart; i < currSheet.getRows(); i++) {
                Cell[] currRow = currSheet.getRow(i);
                for (int j = 0; j < currRow.length; j++) {
                    Cell tCell = currRow[j];
                    String contents = tCell.getContents();
                    try {
                        if (contents.equalsIgnoreCase("^tableStart^")) {
                            pt.setStartCell(tCell.getRow(), tCell.getColumn());
                        }
                        if (contents.equalsIgnoreCase("^tableEnd^")) {
                            //we don't need to do anymore the template data has been copied out
                            pt.setEndCell(tCell.getRow(), tCell.getColumn());
                            tableEnd = true;
                        } else if (contents.equalsIgnoreCase("^rowEnd^")) {
                            pt.setStartCell(tCell.getRow(), 0);
                            pt.setEndCell(tCell.getRow(), tCell.getColumn());
                            pt.setRowEndUsed(true);
                        } else if (contents.startsWith("^") && !tableEnd) {
                            //look for template specific variables that we need to pull out
                            //like ^map, ^color*, ^accountFilter
                            if (contents.toLowerCase().startsWith("^map::")) {
                                handleMap(pt, tCell, currSheet);
                            } else if (contents.toLowerCase().startsWith("^concatcell::")) {
                                handleConcatCell(pt, tCell, currSheet);
                            } else if (contents.toLowerCase().startsWith("^accountfilter^")) {
                                handleAccountFilter(tCell, currSheet);
                            }
                        } else if (!contents.equalsIgnoreCase("^tableStart^") && !contents.equalsIgnoreCase("") && tableStart != null && tCell.getColumn() == tableStart.getColumn()) {
                            handleRowFilter(pt, tCell);
                        } else if (contents.toLowerCase().startsWith("^wildcard^")) {//Find wildCard tag in rows below ^tableStart^
                            handleWildCard(tCell, currSheet);
                        }
                        if (caller.equalsIgnoreCase("sla")) {
                            Cell cell = currSheet.getCell("AN5");
                            handleWildCard(cell, currSheet);
                        }
                        if (caller.equalsIgnoreCase("oldSla") || caller.equalsIgnoreCase("oldSlo")) {
                            Cell cell = currSheet.getCell("Z5");
                            handleWildCard(cell, currSheet);
                        }
                        if (caller.equalsIgnoreCase("analysis")) {
                            Sheet sheet = template.getSheet(1);
                            Cell cell = sheet.getCell("AZ5");
                            handleWildCard(cell, sheet);
                        }
                    } catch (Exception e) {
                        log.error("Template Error (sheet = " + currSheet.getName() + " r=" + tCell.getRow() + " c=" + tCell.getColumn() + ") : " + contents);
                        log.error(e.getMessage(), e);
                    }
                }
            }
            pt.setPageIndex(templateData.size());
            templateData.add(pt);
        }
    }

    //******************************************************************************
    //** Helper methods for the creation of the template mappings
    //******************************************************************************

    /**
     * Find the $tableStart$ cell in the given sheet
     *
     * @param currSheet the workbook sheet to look through
     * @return the cell holding the $tableStart$ or null if it doesn't exist
     */
    private Cell findTableStart(Sheet currSheet) {
        return findCellWithContents(currSheet, "^tableStart^", true, true);
    }

    /**
     * Find the $rowEnd$ cell in the given sheet
     *
     * @param currSheet the workbook sheet to look through
     * @return the cell holding the $rowEnd$ or null if it doesn't exist
     */
    private Cell findRowEnd(Sheet currSheet) {
        return findCellWithContents(currSheet, "^rowEnd^", true, false);
    }

    /**
     * Find a cell within the ROW_LIMIT, COL_LIMIT range that contains the given
     * contents.
     *
     * @param currSheet   the sheet to find the contents in
     * @param contents    the String to match
     * @param useRowLimit use the predefined limits of the rows
     * @param useColLimit use the predefined limits of the cols
     * @return the cell with the contents or null.
     */
    private Cell findCellWithContents(Sheet currSheet, String contents, boolean useRowLimit, boolean useColLimit) {
        int rowEnd = currSheet.getRows();
        if (useRowLimit) {
            rowEnd = lowerValue(currSheet.getRows(), ROW_LIMIT);
        }
        int colEnd = currSheet.getColumns();
        if (useColLimit) {
            colEnd = lowerValue(currSheet.getColumns(), COL_LIMIT);
        }

        for (int i = 0; i < rowEnd; i++) {
            for (int j = 0; j < colEnd; j++) {
                Cell test = currSheet.getCell(j, i);
                if (test.getContents().equalsIgnoreCase(contents)) {
                    return test;
                }
            }
        }

        return null;
    }

    /**
     * Return the lower value of two integers
     *
     * @param val1 the first value
     * @param val2 the second value
     * @return the lower of the two values.
     */
    private int lowerValue(int val1, int val2) {
        if (val1 < val2) {
            return val1;
        } else {
            return val2;
        }
    }

    /**
     * Create the hash table from the template
     *
     * @param pt   the Page Template
     * @param cell the cell that starts the hash map
     * @param s    the sheet the cell is on
     */
    private void handleMap(PageTemplate pt, Cell cell, Sheet s) {
        String content = cell.getContents();
        String name = content.substring(6, content.length()); //should remove the ^map:: and ^ at the end
        //now the rest of the key value pairs are below this cell and go until there is an empty cell
        int startRow = cell.getRow() + 1;
        int col = cell.getColumn();
        Cell key = s.getCell(col, startRow);
        Cell value = s.getCell(col + 1, startRow);
        Hashtable<String, String> kvPairs = new Hashtable<String, String>();

        while (!key.getContents().equalsIgnoreCase("") && startRow < s.getRows()) {
            kvPairs.put(key.getContents(), value.getContents());
            startRow++;
            if (startRow < s.getRows()) {
                key = s.getCell(col, startRow);
                value = s.getCell(col + 1, startRow);
            }
        }
        pt.addHashMap(name, kvPairs);
    }

    /**
     * Create the concatCell table from the template
     *
     * @param pt   the Page Template
     * @param cell the cell that starts the concatCell
     * @param s    the sheet the cell is on
     */
    private void handleConcatCell(PageTemplate pt, Cell cell, Sheet s) {
        String content = cell.getContents();
        String name = content.substring(13, content.length());
        int startRow = cell.getRow() + 1;
        int col = cell.getColumn();
        Cell question = s.getCell(col, startRow);
        Cell answer = s.getCell(col + 1, startRow);
        ArrayList<String> questions = new ArrayList<String>();
        ArrayList<String> answers = new ArrayList<String>();

        while (!answer.getContents().equalsIgnoreCase("") &&
                startRow < s.getRows()) {
            questions.add(question.getContents());
            answers.add(answer.getContents());
            startRow++;
            if (startRow < s.getRows()) {
                question = s.getCell(col, startRow);
                answer = s.getCell(col + 1, startRow);
            }
        }
        pt.addConcatCell(name, questions, answers);
    }

    /**
     * Set up the account filter for the report
     *
     * @param cell the cell containing the account filter header
     * @param s    the sheet that the account filter is on
     */
    private void handleAccountFilter(Cell cell, Sheet s) {
        //the row right below cell will be the function that has to be checked
        int row = cell.getRow() + 1;
        int col = cell.getColumn();
        String contents = s.getCell(col, row).getContents();
        if (!contents.equalsIgnoreCase("")) {
            templateAccountFilter = contents;
        }
    }

    /**
     * Insert the row filter into the Page Template class
     *
     * @param pt   the page template
     * @param cell the cell that contains the row filter
     */
    private void handleRowFilter(PageTemplate pt, Cell cell) {
        String contents = cell.getContents();
        int row = cell.getRow();
        if (!contents.equalsIgnoreCase("")) {
            pt.setRowFilter(row, contents);
        }
    }

    //******************************************************************************
    //** Code to create the report from the template
    //******************************************************************************

    /**
     * Prepares the report Workbook for writing by making a copy
     * of the template Workbook.  The report Workbook is set to be written
     * out to a ByteArrayOutputStream, baos.
     */
    private void prepareExcelWorkbooks() {
        log.debug("prepareExcelWorkbooks().......");
        baos = new ByteArrayOutputStream();
        ByteArrayOutputStream tBaos = new ByteArrayOutputStream();
        try {
            report = Workbook.createWorkbook(baos, template);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        try {
            templateWriteable = Workbook.createWorkbook(tBaos, template);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        log.debug("complete prepareExcelWorkbooks().");
    }


    private void writeReportForAnalysis(List<Contract> contractList) {
        log.debug("writeReportForAnalysis().......");
        //int rowsUsed = 0;
        //TODO: setup the 3 excel workbooks here to have their sheets in a Hashtable to get to them easily
        templateSheets = new Hashtable<Integer, Sheet>();
        reportSheets = new Hashtable<Integer, WritableSheet>();
        writableTemplateSheets = new Hashtable<Integer, WritableSheet>();
        sheetCounters = new Hashtable<String, Integer>();
        log.debug("Starting template, report and writable template cache");
        Date tcS = new Date();
        for (int i = 0; i < templateData.size(); i++) {
            PageTemplate pt = templateData.get(i);
            int rowStart = pt.getStartRow();
            int colStart = pt.getStartCol();
            int rowEnd = pt.getEndRow();
            int colEnd = pt.getEndCol();

            if (!(rowStart == -1 || colStart == -1 || rowEnd == -1 || colEnd == -1)) {
                //only cache the sheets that we can modify in the template
                templateSheets.put(i, template.getSheet(i));
                reportSheets.put(i, report.getSheet(i));
                writableTemplateSheets.put(i, templateWriteable.getSheet(i));
                //TODO: Put sheet numbers here
                sheetCounters.put("" + i, 2);
                log.debug("Cached page " + i + " pt.pageIndex = " + pt.getPageIndex());
            }
        }

        Date tcE = new Date();
        log.debug("Finished template caching, " + (tcE.getTime() - tcS.getTime()) + " ms");
        Integer index = 0;
        // put number of reports to be generated here
        for (Contract contract : contractList) {
            hashtable = new Hashtable<String, String>();
            try {
                log.debug("contract: " + contract.getContractId() + ", contractName: " + contract.getTitle());
                hashtable.put("contractId", "" + contract.getContractId());
                hashtable.put("contractName", contract.getTitle());
                hashtable.put("customer", (contract.getCustomer().getName()) == null ? "" : contract.getCustomer().getName());
                hashtable.put("geo", "");
                hashtable.put("iot", "");
                hashtable.put("imt", "");
                hashtable.put("country", "");


                //hashtable.put("deliveryLoc", contract.getDeliveryLocation());
                hashtable.put("industry", "");
                hashtable.put("sector", "");


            } catch (Exception e) {
                log.error("Error filling Analysis hashtable:  ");
                log.error(e.getMessage(), e);
            }
            log.debug("Total sla included => indexString: " + String.valueOf(index));
            hashtable.put("count", String.valueOf(index));
            Hashtable<String, String> responses = hashtable;
            Hashtable<String, String[]> profileResponses = null;
            Hashtable<String, String[]> businessEntitiesResponses = null;

            Date sTime = new Date();
            writeAccountIntoReport(responses, profileResponses, businessEntitiesResponses);
        }

        log.debug("complete writeReport().");
    }

    private void writeReportForProfile(List<Contract> profileContracts) {
        log.debug("writeReportForProfile().......");
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
        //int rowsUsed = 0;
        //TODO: setup the 5 excel workbooks here to have their sheets in a Hashtable to get to them easily
        templateSheets = new Hashtable<Integer, Sheet>();
        reportSheets = new Hashtable<Integer, WritableSheet>();
        writableTemplateSheets = new Hashtable<Integer, WritableSheet>();
        sheetCounters = new Hashtable<String, Integer>();
        log.debug("Starting template, report and writable template cache");
        Date tcS = new Date();
        for (int i = 0; i < templateData.size(); i++) {
            PageTemplate pt = templateData.get(i);
            int rowStart = pt.getStartRow();
            int colStart = pt.getStartCol();
            int rowEnd = pt.getEndRow();
            int colEnd = pt.getEndCol();

            if (!(rowStart == -1 || colStart == -1 || rowEnd == -1 || colEnd == -1)) {
                //only cache the sheets that we can modify in the template
                templateSheets.put(i, template.getSheet(i));
                reportSheets.put(i, report.getSheet(i));
                writableTemplateSheets.put(i, templateWriteable.getSheet(i));
                //TODO: Put sheet numbers here
                sheetCounters.put("" + i, 1);
                log.debug("Cached page " + i + " pt.pageIndex = " + pt.getPageIndex());
            }
        }

        Date tcE = new Date();
        log.debug("Finished template caching, " + (tcE.getTime() - tcS.getTime()) + " ms");

        //populate Detailed Report data into hashtable
        log.debug("profileContracts: " + profileContracts.size());
        if (CollectionUtils.isNotEmpty(profileContracts)) {
            for (Contract contract : profileContracts) {
                hashtable = new Hashtable<String, String>();
                try {
                    //log.debug("profile detailed report - contract: " + contract.getContractId());
                    hashtable.put("profileContractId", "" + contract.getContractId());
                    hashtable.put("profileContractTitle", StringUtils.isBlank(contract.getTitle()) ? "" : contract.getTitle());
                    hashtable.put("profileCustomer", contract.getCustomer() == null ? "" : contract.getCustomer().getName());
                    hashtable.put("profileState", StringUtils.isBlank(contract.getState()) ? "" : contract.getState());


                    hashtable.put("profileCountry", "");
                    hashtable.put("profiledGeo", "");
                    hashtable.put("profiledIot", "");
                    hashtable.put("profileImt", "");


                    //tower

                    //industry
                    hashtable.put("profileIndustryName", "");
                    hashtable.put("profileIndustrySector", "");

                    hashtable.put("profileStartDate", contract.getStartDate() == null ? "" : dateFormat.format(contract.getStartDate()));
                    hashtable.put("profileEndDate", contract.getEndDate() == null ? "" : dateFormat.format(contract.getEndDate()));
                    hashtable.put("profileDeliveryLocation", contract.getDeliveryLocation() == null ? "" : contract.getDeliveryLocation());
                    hashtable.put("profileUpdatedBy", contract.getGpsUser() == null ? "" : contract.getGpsUser().getEmail());
                    hashtable.put("profileUpdatedOn", contract.getUpdatedOn() == null ? "" : dateFormat.format(contract.getUpdatedOn()));


                    hashtable.put("profileAutoHealthUpdate", contract.getAutoHealthUpdate() == null ? "" : contract.getAutoHealthUpdate());


                    //contacts
                    List<String> contactList = new ArrayList<String>();
                    if (CollectionUtils.isNotEmpty(contract.getContractContacts())) {
                        for (ContractContact contractContact : contract.getContractContacts()) {
                            if (contractContact.getContactType() != null) {
                                StringBuilder contractDetails = new StringBuilder();
                                contractDetails.append("- " + contractContact.getContactType().getTitle() + ": ");
                                if (!StringUtils.isBlank(contractContact.getContactName())) {
                                    contractDetails.append(contractContact.getContactName() + ", ");
                                }
                                if (!StringUtils.isBlank(contractContact.getContactIntranetId())) {
                                    contractDetails.append(contractContact.getContactIntranetId() + ", ");
                                }
                                if (!StringUtils.isBlank(contractContact.getContractPhone())) {
                                    contractDetails.append(contractContact.getContractPhone());
                                }
                                String contractDetailsStr = "";
                                if (!StringUtils.isBlank(contractDetails.toString())) {
                                    contractDetailsStr = removeLastCommaChar(contractDetails.toString());
                                }
                                if (!contactList.contains(contractDetailsStr)) {
                                    contactList.add(contractDetailsStr);
                                }
                            }
                        }
                    }
                    hashtable.put("profileContacts", CollectionUtils.isEmpty(contactList) ? "" : StringUtils.join(contactList, "\n"));


                    //bcr
                    List<String> bcrReviewsList = new ArrayList<String>();
                    hashtable.put("profileBcrReviews", CollectionUtils.isEmpty(bcrReviewsList) ? "" : StringUtils.join(bcrReviewsList, "\n"));


                } catch (Exception e) {
                    log.error("Error filling Profile hashtable for Detailed Report:  ");
                    log.error(e.getMessage(), e);
                }
                if (!hashtable.isEmpty()) {
                    Hashtable<String, String> responses = hashtable;
                    Hashtable<String, String[]> profileResponses = null;
                    Hashtable<String, String[]> businessEntitiesResponses = null;

                    Date sTime = new Date();
                    writeAccountIntoReport(responses, profileResponses, businessEntitiesResponses);
                }
            }
        }

        log.debug("complete writeReport().");
    }

    private String removeLastCommaChar(String contractDetails) {
        String lastWord = contractDetails.substring(contractDetails.length() - 2, contractDetails.length() - 1);
        if (lastWord.equalsIgnoreCase(",")) {
            return contractDetails.substring(0, contractDetails.length() - 1);
        }
        return contractDetails;
    }


    private void writeAccountIntoReport(Hashtable<String, String> responses, Hashtable<String, String[]> profileResponses, Hashtable<String, String[]> businessEntitiesResponses) {
        log.info("inside writeAccountIntoReport");
        int currRowsUsed = 0;
        //int pageCount = 0;
        int addVal = 0;
        int rowsToAdd = 0;
        int currPage = 0;
        int oldVal = 0;
        int initCountVal = 0;
        //write the account that is selected into the report.. assuming
        //it passes the template account filter
        String sheetHash = "";
        log.info("templateData.size(): " + templateData.size());
        for (currPage = 0; currPage < templateData.size(); currPage++) {
            Date pSTime = new Date();
            PageTemplate pt = templateData.get(currPage);
            //int slaIndex = 0;
            String associatedWith = "";
            if (responses.get("associatedWith") != null) {
                associatedWith = responses.get("associatedWith").toString();
            }
            String[] associatedWiths = associatedWith.split(",");
            ArrayList<String> slaList = new ArrayList<String>();
            ArrayList<String> sloList = new ArrayList<String>();
            ArrayList<String> miList = new ArrayList<String>();
            for (int assoc = 0; assoc < associatedWiths.length; assoc++) {
                if (associatedWiths[assoc].contains("SLA")) {
                    slaList.add(associatedWiths[assoc]);
                } else if (associatedWiths[assoc].contains("SLO")) {
                    sloList.add(associatedWiths[assoc]);
                } else if (associatedWiths[assoc].contains("MI")) {
                    miList.add(associatedWiths[assoc]);
                }
            }
            int rowStart = pt.getStartRow();
            int colStart = pt.getStartCol();
            int rowEnd = pt.getEndRow();
            int colEnd = pt.getEndCol();

            if (rowStart == -1 || colStart == -1 || rowEnd == -1 || colEnd == -1) {
                Date pETime = new Date();
                log.debug("  Page " + currPage + " (skipped) time " + (pETime.getTime() - pSTime.getTime()) + " ms");
                continue; //skip this page since there is nothing on it.
            }

            //TODO: make these read the data from a hashtable
            Sheet currTemplateSheet = templateSheets.get(currPage);//template.getSheet(currPage);
            WritableSheet currReportSheet = reportSheets.get(currPage);//report.getSheet(currPage);
            WritableSheet currTemplateWSheet = writableTemplateSheets.get(currPage);//templateWriteable.getSheet(currPage);
            Range[] mergedCells = currTemplateWSheet.getMergedCells();
            if (rowStart != rowEnd) {
                for (int x = 0; x < rowEnd; x++) {
                    try {
                        currReportSheet.setRowView(x, false);
                    } catch (RowsExceededException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            int rowOffset = /*pt.getStartRow() +*/ /*rowsUsed*/ /* (pt.getEndRow() - pt.getStartRow())*/ 0;
            if (sheetCounters.containsKey("" + currPage)) {
                rowOffset = oldVal = ((Integer) sheetCounters.get("" + currPage)).intValue();
            }

            sheetHash = currTemplateSheet.getName() + "";
            //pageCount ++;
            for (int currRow = rowStart; currRow <= rowEnd; currRow++) {
                //check for the row filter
                String rowFilter = pt.getRowFilter(currRow);
                boolean doRow = true;
                //test the row filter

                if (doRow == false) { //since we aren't using a row, need to fix the row offset
                    //clear the row
                    for (int clR = 0; clR < currReportSheet.getColumns(); clR++) {
                        //TODO: the calculation of the row needs to be changed to account for tables
                        jxl.write.Label tL = null;
                        tL = new jxl.write.Label(clR, currRow + rowOffset, "");
                        try {
                            currReportSheet.addCell(tL);
                        } catch (RowsExceededException e) {
                            log.error(e.getMessage());
                        } catch (WriteException e) {
                            log.error(e.getMessage());
                        }
                    }
                    wildKeyIndex++;
                    rowOffset--;
                }
                if (doRow) {
                    boolean wildRowProcessed = false;
                    addVal = 1;
                    String key = "";
                    Integer wildRownNum = new Integer(-1);
                    if (wildKeyIndex < wildKeys.size()) {
                        key = wildKeys.get(wildKeyIndex);
                        wildRownNum = wildCardCounts.get(key);
                    }
                    if (currRow == wildRownNum.intValue()) {

                        try {
                            if (key.startsWith("@$") && key.endsWith("$@")) {
                                rowsToAdd = initCountVal;
                            } else {
                                rowsToAdd = Integer.parseInt(handleDollarSign(key, responses));
                                initCountVal = rowsToAdd;
                            }
                        } catch (NumberFormatException e) {
                            rowsToAdd = 1;
                        }
                        wildRowProcessed = true;

                    } else {
                        rowsToAdd = 1;
                    }
                    List<Integer> errorColumns = new ArrayList<Integer>();
                    for (int cRow = 1; cRow <= rowsToAdd; cRow++) {
                        //int rowHeight = 1;
                        for (int currCol = colStart; currCol <= colEnd; currCol++) {
                            String newContents = "";
                            // get cell from template and then copy to report
                            Cell cell = currTemplateSheet.getCell(currCol, currRow);
                            // right here, we need to get the cell format data
                            // from the template cell to the target cell
                            WritableCell source = currTemplateWSheet
                                    .getWritableCell(currCol, currRow); // currReportSheet.getWritableCell(currCol,currRow);
                            WritableCell target = source.copyTo(currCol,
                                    rowOffset + currRow);// currReportSheet.getWritableCell(currCol,rowOffset+currRow);//source.copyTo(currCol,rowOffset+currRow);
                            if (source.getCellFormat() != null)
                                target.setCellFormat(new WritableCellFormat(
                                        source.getCellFormat()));

                            // now based on the contents of the ct, determine
                            // what to do w/ the target
                            String contents = cell.getContents();

                            if (rowFilter != null && currCol == 0) {
                                // remove the rowFilter from being shown
                                contents = "";
                            }

                            if (contents.startsWith("&")) {
                                newContents = handleAmpersand(contents, responses, profileResponses, businessEntitiesResponses);
                            } else if (contents.startsWith("$")) {
                                if (contents.contains("+")) {
                                    newContents = handleDollarSign(contents, responses);
                                }
                                if (contents.contains("*")) {
                                    newContents = handleAsterickCR(contents, responses, cRow);
                                } else {
                                    newContents = handleDollarSign(contents,
                                            responses);
                                }
                            } else {
                                newContents = contents;
                            }

                            if (newContents == null) {
                                if (!errorColumns.contains(currCol)) {
                                    log.warn("newContents is null.... an error expected [contents=" + contents + ", currCol=" + currCol + ", currRow=" + currRow + "]");
                                    errorColumns.add(currCol);
                                }
                                newContents = "";
                            }

                            if (newContents.length() > EXCEL_CELL_LIMIT) {
                                newContents = newContents.substring(0, EXCEL_CELL_LIMIT);
                            }

                            int contentRows = newContents.split("\r").length;
                            //if( contentRows > rowHeight)
                            //rowHeight = contentRows;

                            if (cell.getType() == CellType.NUMBER_FORMULA
                                    || cell.getType() == CellType.STRING_FORMULA
                                    || cell.getType() == CellType.BOOLEAN_FORMULA
                                    || cell.getType() == CellType.FORMULA_ERROR) {
                                try {
                                    currReportSheet.addCell(target);
                                } catch (RowsExceededException e) {
                                    log.error(e.getMessage(), e);
                                } catch (WriteException e) {
                                    log.error(e.getMessage(), e);
                                } catch (Exception e) {
                                    //TODO:removed log.error here
                                }
                            } else if (isStringANumber(newContents)) {
                                jxl.write.Number nCell = new jxl.write.Number(
                                        target.getColumn(), target.getRow(),
                                        Double.parseDouble(newContents));
                                if (target.getCellFormat() != null)
                                    nCell.setCellFormat(target.getCellFormat());

                                try {
                                    currReportSheet.addCell(nCell);
                                } catch (RowsExceededException e) {
                                    log.error(e.getMessage(), e);
                                } catch (WriteException e) {
                                    log.error(e.getMessage(), e);
                                } catch (Exception e) {
                                    log.error("Fatal error: " + e.getMessage());
                                }
                            } else {
                                jxl.write.Label lCell = new Label(target.getColumn(), target.getRow(), newContents);
                                if (target.getCellFormat() != null)
                                    lCell.setCellFormat(target.getCellFormat());

                                try {
                                    currReportSheet.addCell(lCell);
                                } catch (RowsExceededException e) {
                                    log.error(e.getMessage(), e);
                                } catch (WriteException e) {
                                    log.error(e.getMessage(), e);
                                } catch (Exception e) {
                                    log.error("Exception: " + e.getMessage());
                                }
                            }

                        }
                        for (int mLen = 0; mLen < mergedCells.length; mLen++) {
                            Range r = mergedCells[mLen];
                            Cell topLeft = r.getTopLeft();
                            Cell botRight = r.getBottomRight();
                            if (topLeft.getRow() == currRow) {
                                try {
                                    currReportSheet.mergeCells(topLeft.getColumn(), topLeft.getRow() + rowOffset, botRight.getColumn(), botRight.getRow() + rowOffset);
                                } catch (RowsExceededException e) {
                                    log.error(e.getMessage(), e);
                                } catch (WriteException e) {
                                    log.error(e.getMessage(), e);
                                }
                            }
                        }

	                      /*try {
                        if(rowHeight>1)
	    					currReportSheet.setRowView(currRow+rowOffset, (rowHeight * 13 * 500), false);
					} catch (Exception e) {
						log.error(com.gps.util.StringUtils.getStackTraceAsString(e));
					}*/

                        if (wildRowProcessed)
                            rowOffset++;
                        currRowsUsed++;

                    }
                    if (wildRowProcessed) {
                        rowOffset--;
                        addVal = 0;
                    }
                    // test to see if the row we are currently copying has the start of
                    // a
                    // merged cell in it. If so, merge the cells with the offsets.


                } else
                    addVal = 0;
            }
            //clean up unused rows here.
            if (rowStart != rowEnd) {
                //determine what the last row we wrote on was, then clean the rows that would still be shown.
                if (rowOffset < 0) {
                    //we need to clear some rows
                    for (int i = 0; i > rowOffset; i--) {
                        //get the row @ rowEnd + i

                        for (int clR = 0; clR < currReportSheet.getColumns(); clR++) {
                            jxl.write.Label tL = null;
                            tL = new jxl.write.Label(clR, rowEnd + i, "");
                            try {
                                currReportSheet.addCell(tL);
                            } catch (RowsExceededException e) {
                                log.error(e.getMessage());
                            } catch (WriteException e) {
                                log.error(e.getMessage());
                            }
                        }

                    }
                }
            }
            Date pETime = new Date();
            if (rowStart == rowEnd)
                sheetCounters.put("" + (currPage), (currRowsUsed + (oldVal - 1)) + addVal);
            else
                sheetCounters.put("" + (currPage), (currRowsUsed + (oldVal)) + 0);
            currRowsUsed = 0;

        }

        //sheetCounters.put(""+(currPageVal), (currRowsUsed + (oldVal-1)) + addVal);
        //return (currRowsUsed - pageCount) + addVal;
    }

    //******************************************************************************
    //** Methods to handle the values found in the cells
    //******************************************************************************

    private void removeEmptyRows() {
        for (int i = 0; i < templateWriteable.getNumberOfSheets(); i++) {
            WritableSheet sheet = templateWriteable.getSheet(i);
            sheet.removeRow(5);
            sheet.removeRow(2);
            sheet.removeRow(4);
            sheet.removeRow(1);
            try {
                sheet.setRowView(6, 20, true);
            } catch (RowsExceededException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
                /*for(int y=1000; y<1; y--){
                    Cell cell = sheet.getCell("A"+y);
					log.debug("cell.getContents: "+cell.getContents());
					if(StringUtils.isNullOrEmpty(cell.getContents().trim())){
						log.debug("removing row "+y+" of sheet "+i);
						sheet.removeRow(y);
					}
				}*/
        }
    }

    /**
     * @param contents         the value to parse and return a new value for
     * @param responses        the list of responses in the questionnaire
     * @param profileResponses the list of responses in the profile (if not the
     *                         questionnaire being processed)
     * @return the value
     */
    private String handleAmpersand(String contents,
                                   Hashtable<String, String> responses,
                                   Hashtable<String, String[]> profileResponses,
                                   Hashtable<String, String[]> businessEntitiesResponses) {

        String newContent = "[unknown]";
        try {
            String table = contents.substring(1, contents.indexOf("$"));
            String varname = contents.substring(contents.indexOf("$") + 1, contents.length() - 1);

            if (table.equalsIgnoreCase("prf") && profileResponses != null) {
                String[] temp = profileResponses.get(varname);
                newContent = convertStringArrayToString(temp);
            } else if (table.equalsIgnoreCase("table")) {
                String temp = responses.get(contents.toLowerCase());
                newContent = temp;
            } else if (table.equalsIgnoreCase("businessentities")) {
                String[] temp = businessEntitiesResponses.get(varname);
                newContent = convertStringArrayToString(temp);
            } else if (table.startsWith("status")) {
                String referData = responses.get("REFER");
                String Pn$Sn = contents.substring(8, contents.length() - 1);
                if (Pn$Sn.contains("$")) {
                    String Pn = Pn$Sn.substring(0, Pn$Sn.indexOf("$"));
                    String Sn = Pn$Sn.substring(Pn$Sn.indexOf("$") + 1);
                }
                //newContent = convertStringArrayToString(responses.get(contents.toLowerCase()));
            }
        } catch (Exception e) {
            log.error("Error in handleAmpersand: " + contents);
            log.error(e.getMessage());
            newContent = "[ERROR]";
        }
        return newContent;
    }

    //******************************************************************************
    //** Helper methods to convert to string or empty string if null
    //******************************************************************************
    private String emptyIfNull(Object value) {
        String result = "";
        if (value == null) {
            return result;
        }
        result = String.valueOf(value);
        return result;
    }

    private String handleAsterickCR(String contents, Hashtable<String, String> responses, int cRow) {
        contents = contents.substring(1, contents.length() - 1);
        contents = contents.replace("*", cRow + "");
        String cResponse = responses.get(contents);
        String cResp = "";
        if (cResponse != null) {
            cResp = cResponse;
        }

        return cResp;
    }

    /**
     * Handles getting the variable out of the questionnaire response
     *
     * @param contents  the contents of the cell
     * @param responses the responses
     * @return the response for the given variable name
     */
    private String handleDollarSign(String contents, Hashtable<String, String> responses) {
        contents = contents.substring(1, contents.length() - 1);
        String cResponse;

//	    log.debug("in handle dollar sign");
        cResponse = responses.get(contents);
        String cResp = "";
        if (cResponse != null) {
            cResp = cResponse;
        }
//	    log.debug("cResp :"+cResp);
        return cResp;
    }

    private void handleWildCard(Cell cell, Sheet s) {
        //the row right next to the cell will be the function that has to be checked.
//		  	log.debug("In handleWildCard...");
        wildCardCounts = new Hashtable<String, Integer>();
        int row = cell.getRow();
        int col = cell.getColumn() + 1;
        int i = 0;
        String contents = "";
        if (s.getCell(col, row) != null) {
            contents = s.getCell(col, row).getContents();
        }
//		    log.debug("contents: "+contents);

        String tempKey = "@$tempKey";
        int j = 1;
        while (!contents.equalsIgnoreCase("") && !contents.equalsIgnoreCase(" ")) {

            if (wildCardCounts.containsKey(contents.toString())) {
                contents = tempKey + j + "$@";
                j++;
                wildCardCounts.put(contents, new Integer(row));
                wildKeys.add(i, contents);
            } else {
                wildCardCounts.put(contents, new Integer(row));
                wildKeys.add(i, contents);
            }
            i++;
            row++;
//		    	log.debug("Info(sheet = "+s.getName()+" row="+cell.getRow()+" col="+cell.getColumn()+") : contents=[" + contents+ "]  wildCardCounts="+wildCardCounts.values()+ "  wildKeys="+wildKeys);
            contents = s.getCell(col, row).getContents();

        }
    }

    /**
     * Return the position in the Array that a String exists
     *
     * @param ar  the String array to check
     * @param val the value to find
     * @return the position of the value in the array, -1 if not found
     */
    private int stringArrayContains(String[] ar, String val) {
        if (ar == null)
            return -1;
        for (int i = 0; i < ar.length; i++) {
            if (ar[i].equals(val))
                return i;
        }
        return -1;
    }

    //******************************************************************************
    //** Helper methods for the jbeans:: and func:: cells
    //******************************************************************************

    /**
     * Reformats a String for the func:: calls
     */
    private String reformat(String s) {
        String patternStr = "[kK]";
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(s);
        s = matcher.replaceAll("000");

        patternStr = "[mM]";
        pattern = Pattern.compile(patternStr);
        matcher = pattern.matcher(s);
        s = matcher.replaceAll("000000");

        patternStr = ",";
        pattern = Pattern.compile(patternStr);
        matcher = pattern.matcher(s);
        s = matcher.replaceAll("");

        patternStr = "\"";
        pattern = Pattern.compile(patternStr);
        matcher = pattern.matcher(s);
        s = matcher.replaceAll("");

        if (s.length() > 0 && !Character.isDigit(s.charAt(0))) {
            s = s.substring(1);

        }
        return s;
    }

    /**
     * @return the String with all the functions completed.
     */

    /**
     * Used in the runInternalFunctions, it cleans up the String for use in
     * the rest of the beanshell code
     *
     * @param result the result to clean
     * @return the cleaned result
     */
    private String cleanUpStringResult(String result) {
        result = result.replace("\\", "\\\\"); // fix \\
        result = result.replace("\r", ""); // fix \r
        result = result.replace("\"", "\\\""); //fix double quotes into characters
        String[] findArray = result.split("\n"); // fix multiline
        if (findArray.length > 1) {
            result = findArray[0].trim();
            for (int q = 1; q < findArray.length; q++) {
                result += "\\n" + findArray[q].trim();
            }
        }
        return result;
    }

    //******************************************************************************
    //** Helper methods for the spreadsheet writing
    //******************************************************************************

    /**
     * Given a String array of responses return the value/s as a csv
     *
     * @param response the response to convert
     * @return the String array as a string
     */
    private String convertStringArrayToString(String[] response) {
        if (response != null) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < response.length; i++) {
                sb.append(response[i]);
                if (i + 1 < response.length)
                    sb.append(",");
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    /**
     * Determine if a String is a number
     *
     * @param s the String to test
     * @return true if it is a number
     */
    private boolean isStringANumber(String s) {
        boolean isNum;
        try {
            double d = Double.parseDouble(s);
            isNum = true;
        } catch (NumberFormatException e) {
            isNum = false;
        }

        return isNum;
    }

    /**
     * Closes all the files used and writes the finished report to the byte[]
     */
    private void writeReportToBytes() {
        template.close();
        try {
            report.write();
            report.close();
            finishedReport = baos.toByteArray();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            finishedReport = null;
        } catch (WriteException e) {
            log.error(e.getMessage(), e);
            finishedReport = null;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            finishedReport = null;
        }

        try {
            templateWriteable.close();
        } catch (IOException e) {
            log.error(e.getMessage());
        } catch (WriteException e) {
            log.error(e.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public byte[] generateRcaCoordinatorReport(List<Rca> rcaListings, String path) {
        log.debug("generateRcaCoordinatorReport()..........");
        try {

            Timestamp dateTimestamp = new Timestamp(new Date().getTime());
            String templateName = path + "RcaCoordinator.xls";
            log.debug("reading xls template: " + templateName);
            File xlsTemplate = new File(templateName);
//            log.debug("creating workbook.....");

            template = Workbook.getWorkbook(xlsTemplate);
            Path xlsPath = Paths.get(templateName);
            byte[] data = Files.readAllBytes(xlsPath);
            // template = getWorkbookFromBytes(data);
//            log.debug("Filling properties in a hashtable.......");

//            log.debug("Prepare excel workbooks.......");
            prepareExcelWorkbooksCR();
            createTemplateMappingsCR();
            writeReportForCR(rcaListings);
            writeReportToBytes();

        } catch (BiffException e) {
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        log.debug("end generateRcaCoordinatorReport()");

        return finishedReport;

    }

    public byte[] generateRcaSummaryReport(List<Rca> rcaListings, String path) {
        log.debug("generateRcaSummaryReport()..........");
        try {
            String templateName = path + "RcaSummary.xls";
            log.debug("reading xls template: " + templateName);
            File xlsTemplate = new File(templateName);
            log.debug("creating workbook.....");
            template = Workbook.getWorkbook(xlsTemplate);
            log.debug("Filling properties in a hashtable.......");

            log.debug("Prepare excel workbooks.......");
            prepareExcelWorkbooks();
            createTemplateMappings("RCASummary");
            writeReportForRcaSummary(rcaListings);
            writeReportToBytes();

        } catch (BiffException e) {
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        log.debug("end generateAnalysisReport()");
        return finishedReport;

    }

    private void writeReportForRcaReport(List<Rca> rcaListings) {

        log.debug("writeReportForRcaReport().......");
        //int rowsUsed = 0;
        //TODO: setup the 3 excel workbooks here to have their sheets in a Hashtable to get to them easily
        templateSheets = new Hashtable<Integer, Sheet>();
        reportSheets = new Hashtable<Integer, WritableSheet>();
        writableTemplateSheets = new Hashtable<Integer, WritableSheet>();
        sheetCounters = new Hashtable<String, Integer>();
        log.debug("Starting template, report and writable template cache");
        Date tcS = new Date();
        for (int i = 0; i < templateData.size(); i++) {
            PageTemplate pt = templateData.get(i);
            int rowStart = pt.getStartRow();
            int colStart = pt.getStartCol();
            int rowEnd = pt.getEndRow();
            int colEnd = pt.getEndCol();

            if (!(rowStart == -1 || colStart == -1 || rowEnd == -1 || colEnd == -1)) {
                //only cache the sheets that we can modify in the template
                templateSheets.put(i, template.getSheet(i));
                reportSheets.put(i, report.getSheet(i));
                writableTemplateSheets.put(i, templateWriteable.getSheet(i));
                //TODO: Put sheet numbers here
                sheetCounters.put("" + i, 1);
                log.debug("Cached page " + i + " pt.pageIndex = " + pt.getPageIndex());
            }
        }

        Date tcE = new Date();
        log.debug("Finished template caching, " + (tcE.getTime() - tcS.getTime()) + " ms");
        // put number of reports to be generated here
        for (Rca rca : rcaListings) {
            int index = 1;
            hashtable = new Hashtable<String, String>();
            try {
                hashtable.put("rcaNumber", String.valueOf(rca.getRcaNumber()));
                hashtable.put("contractTitle", rca.getContract().getTitle());
                hashtable.put("rcaTitle", rca.getTitle());
                hashtable.put("rcaCreateDate", rca.getCreateDate() != null ? CommonUtil.convertDateToString(rca.getCreateDate()) : "");
                hashtable.put("rcaEndDate", rca.getDueDate() != null ? CommonUtil.convertDateToString(rca.getDueDate()) : "");
                hashtable.put("rcaClosedDate", rca.getCloseDate() != null ? CommonUtil.convertDateToString(rca.getCloseDate()) : "");
                hashtable.put("rcaStage", rca.getRcaStage().equalsIgnoreCase("ClosedWithOpenActions") ? "Closed with Open Actions" : rca.getRcaStage());
                hashtable.put("rcaCoordinator", rca.getPrimaryRcaCoordinator() != null ? BluePages.getNotesIdFromIntranetId(rca.getPrimaryRcaCoordinator().getCoordinator().getIntranetId()) : "");
                hashtable.put("actionItemsOpen", rca.getActionItemOpen() != null ? String.valueOf(rca.getActionItemOpen()) : "");
                hashtable.put("actionItemsClosed", rca.getActionItemClosed() != null ? String.valueOf(rca.getActionItemClosed()) : "");
                hashtable.put("problemDescription", StringUtils.isNotBlank(rca.getRcaEventDetail().getIssueDescription()) ? rca.getRcaEventDetail().getIssueDescription() : "");
                hashtable.put("customerImpacted", StringUtils.isNotBlank(rca.getCustomerImpacted()) ? rca.getCustomerImpacted() : "");
                hashtable.put("othercustomerImpacted", StringUtils.isNotBlank(rca.getCustomerOther()) ? rca.getCustomerOther() : "");
//                log.info("othercustomerImpacted = "+rca.getCustomerOther());
                hashtable.put("rcaOwner", StringUtils.isNotBlank(rca.getRcaOwner()) ? BluePages.getNotesIdFromIntranetId(rca.getRcaOwner()) : "");
                hashtable.put("rcaDelegate", StringUtils.isNotBlank(rca.getRcaDelegate()) ? BluePages.getNotesIdFromIntranetId(rca.getRcaDelegate()) : "");
                index++;
            } catch (Exception e) {
                log.error("Error filling RCA Coordinator hashtable:  ");
                log.error(e.getMessage(), e);
            }

            hashtable.put("count", String.valueOf(index));
            Hashtable<String, String> responses = hashtable;
            Hashtable<String, String[]> profileResponses = null;
            Hashtable<String, String[]> businessEntitiesResponses = null;

            Date sTime = new Date();
            writeAccountIntoReport(responses, profileResponses, businessEntitiesResponses);
        }
        log.debug("complete writeReport().");


    }


    private void writeReportForRcaSummary(List<Rca> rcaListings) {

        log.debug("writeReportForRcaSummary().......");
        //int rowsUsed = 0;
        //TODO: setup the 3 excel workbooks here to have their sheets in a Hashtable to get to them easily
        templateSheets = new Hashtable<Integer, Sheet>();
        reportSheets = new Hashtable<Integer, WritableSheet>();
        writableTemplateSheets = new Hashtable<Integer, WritableSheet>();
        sheetCounters = new Hashtable<String, Integer>();
        log.debug("Starting template, report and writable template cache");
        Date tcS = new Date();
        for (int i = 0; i < templateData.size(); i++) {
            PageTemplate pt = templateData.get(i);
            int rowStart = pt.getStartRow();
            int colStart = pt.getStartCol();
            int rowEnd = pt.getEndRow();
            int colEnd = pt.getEndCol();

            if (!(rowStart == -1 || colStart == -1 || rowEnd == -1 || colEnd == -1)) {
                //only cache the sheets that we can modify in the template
                templateSheets.put(i, template.getSheet(i));
                reportSheets.put(i, report.getSheet(i));
                writableTemplateSheets.put(i, templateWriteable.getSheet(i));
                //TODO: Put sheet numbers here
                sheetCounters.put("" + i, 1);
                log.debug("Cached page " + i + " pt.pageIndex = " + pt.getPageIndex());
            }
        }


        Date tcE = new Date();
        log.debug("Finished template caching, " + (tcE.getTime() - tcS.getTime()) + " ms");
        // put number of reports to be generated here
        for (Rca rca : rcaListings) {
            int index = 1;
            hashtable = new Hashtable<String, String>();
            try {

                // RCA Data sheet values

                hashtable.put("accountid", String.valueOf(rca.getContract().getContractId()));
                hashtable.put("accountName", rca.getContract().getTitle());
                hashtable.put("rcaId", String.valueOf(rca.getRcaNumber()));
                hashtable.put("RCAtitle", rca.getTitle());
                hashtable.put("rcaAssociatedWith", rca.getRcaAssociatedWith());
                hashtable.put("associatedWith", rca.getRcaAssociatedWith());
                hashtable.put("rcaCreationDate", rca.getCreateDate() != null ? CommonUtil.convertDateToStringFormat(rca.getCreateDate()) : "");
                hashtable.put("rcaEndDate", rca.getDueDate() != null ? CommonUtil.convertDateToStringFormat(rca.getDueDate()) : "");
                hashtable.put("rcaApprovedDate", rca.getRcaHelper().getApprovedDate() != null ? CommonUtil.convertDateToStringFormat(rca.getRcaHelper().getApprovedDate()) : "");
                hashtable.put("rcaCloDate", rca.getCloseDate() != null ? CommonUtil.convertDateToStringFormat(rca.getCloseDate()) : "");
                hashtable.put("rcaStatus", rca.getRcaStage());
                hashtable.put("RCACoordname", BluePages.getNotesIdFromIntranetId(rca.getRcaCoordinatorId()));
                hashtable.put("actionsOpened", rca.getActionItemOpen() != null ? String.valueOf(rca.getActionItemOpen()) : "");
                hashtable.put("actionsClosed", rca.getActionItemClosed() != null ? String.valueOf(rca.getActionItemClosed()) : "");
                hashtable.put("rcaCreatedBy", rca.getRcaCreatedBy() != null ? BluePages.getNotesIdFromIntranetId(rca.getRcaCreatedBy().getEmail()) : "");
                hashtable.put("ownersList", StringUtils.isNotBlank(rca.getRcaOwner()) ? BluePages.getNotesIdFromIntranetId(rca.getRcaOwner()) : "");
                hashtable.put("delegateList", StringUtils.isNotBlank(rca.getRcaDelegate()) ? BluePages.getNotesIdFromIntranetId(rca.getRcaDelegate()) : "");
                hashtable.put("additionalEditorList", getRcaEditors(rca));
                hashtable.put("dpeList", BluePages.getNotesIdFromIntranetId(rca.getRcaHelper().getRcaDpe().getEmail()));
                hashtable.put("rcaAccepted", (StringUtils.isBlank(rca.getIsOwnerAccepted()) || (com.gps.util.StringUtils.isNotBlank(rca.getIsOwnerAccepted()) && rca.getIsOwnerAccepted().trim().equalsIgnoreCase("N"))) ? "No" : "Yes");
                hashtable.put("rcaCoordManager", StringUtils.isNotBlank(rca.getRcaHelper().getRcaCoordinatorManager()) ? rca.getRcaHelper().getRcaCoordinatorManager() : "");
                hashtable.put("assigneeGroup", "");


                hashtable.put("rcaAcceptedDelegate", (StringUtils.isBlank(rca.getIsDelegateAccepted()) || (com.gps.util.StringUtils.isNotBlank(rca.getIsDelegateAccepted()) && rca.getIsDelegateAccepted().trim().equalsIgnoreCase("N"))) ? "No" : "Yes");
                hashtable.put("outageStartDate", rca.getIncidentStartTime() != null ? CommonUtil.convertTimestampToString(rca.getIncidentStartTime()) : "");
                hashtable.put("outageEndDate", rca.getIncidentStartTime() != null ? CommonUtil.convertTimestampToString(rca.getIncidentEntTime()) : "");
                hashtable.put("outageDuration", StringUtils.isNotBlank(rca.getIncidentDuration()) ? rca.getIncidentDuration() : "");
                hashtable.put("primaryTicketExists", StringUtils.isNotBlank(rca.getProblemIndidentRecord()) ? "Yes" : "No");
                hashtable.put("primaryTicket", StringUtils.isNotBlank(rca.getProblemIndidentRecord()) ? rca.getProblemIndidentRecord() : "");
                hashtable.put("primaryTicketType", StringUtils.isNotBlank(rca.getPrimaryTicketType()) ? rca.getPrimaryTicketType() : "");
                hashtable.put("associatedToolPrimary", StringUtils.isNotBlank(rca.getPrimaryTicketAssociation()) ? rca.getPrimaryTicketAssociation() : "");
                hashtable.put("severity", rca.getSeverity() != null ? String.valueOf(rca.getSeverity()) : "");
                hashtable.put("inciDetail", StringUtils.isNotBlank(rca.getRcaEventDetail().getIssueDescription()) ? rca.getRcaEventDetail().getIssueDescription() : "");
                hashtable.put("chronOfEvents", StringUtils.isNotBlank(rca.getRcaEventDetail().getChronology()) ? rca.getRcaEventDetail().getChronology() : "");
                hashtable.put("servRestore", StringUtils.isNotBlank(rca.getRcaEventDetail().getHowServiceRestored()) ? rca.getRcaEventDetail().getHowServiceRestored() : "");
                hashtable.put("repeatInci", StringUtils.isNotBlank(rca.getRcaEventDetail().getRepeatIssue()) ? rca.getRcaEventDetail().getRepeatIssue() : "");
                hashtable.put("repeatRCANumber", StringUtils.isNotBlank(rca.getRcaEventDetail().getRepeatRcaOrTicketNum()) ? rca.getRcaEventDetail().getRepeatRcaOrTicketNum() : "");

                hashtable.put("supportTeam", StringUtils.isNotBlank(rca.getRcaEventDetail().getSupportTeamEffectiveness()) ? rca.getRcaEventDetail().getSupportTeamEffectiveness() : "");
                hashtable.put("repeatRisk", StringUtils.isNotBlank(rca.getRcaEventDetail().getRepeatRisk()) ? rca.getRcaEventDetail().getRepeatRisk() : "");
                hashtable.put("execAlert", StringUtils.isNotBlank(rca.getRcaEventDetail().getExecutiveAlert()) ? rca.getRcaEventDetail().getExecutiveAlert() : "");

                hashtable.put("locImpact", StringUtils.isNotBlank(rca.getLocationOfFailure()) ? rca.getLocationOfFailure() : "");
                hashtable.put("OS", StringUtils.isNotBlank(rca.getRcaEventDetail().getImpactedTower()) ? rca.getRcaEventDetail().getImpactedTower() : "");
                //  hashtable.put("additionalComments", StringUtils.isNotBlank(rca.getRcaEventDetail().getAdditionalCommentIbm()) ? rca.getRcaEventDetail().getAdditionalCommentIbm() : "");
                hashtable.put("keyIssues", StringUtils.isNotBlank(rca.getRcaEventDetail().getKeyIssues()) ? rca.getRcaEventDetail().getKeyIssues() : "");

                //hashtable.put("businessImpactDetails", StringUtils.isNotBlank(rca.getImpactDetails()) ? rca.getImpactDetails() : "");

                hashtable.put("serviceImpacted", StringUtils.isNotBlank(rca.getServiceImpacted()) ? rca.getServiceImpacted() : "");
                hashtable.put("systemServerName", StringUtils.isNotBlank(rca.getSystemImpacted()) ? rca.getSystemImpacted() : "");
                hashtable.put("customerImpacted", StringUtils.isBlank(rca.getCustomerImpacted()) || rca.getCustomerImpacted().equalsIgnoreCase("N") ? "No" : "Yes");
                hashtable.put("othercustomerImpacted", StringUtils.isNotBlank(rca.getCustomerOther()) ? rca.getCustomerOther() : "");
//                log.info("othercustomerImpacted = "+rca.getCustomerOther());
                hashtable.put("managedBy", rca.getManagedBy());
                hashtable.put("impactCust", StringUtils.isNotBlank(rca.getCustomerImpactedList()) ? rca.getCustomerImpactedList() : "");
                hashtable.put("dpeNotesId", rca.getRcaHelper().getRcaDpe() != null ? rca.getRcaHelper().getRcaDpe().getNotesId() : "");
                hashtable.put("reasonRCARequired", StringUtils.isNotBlank(rca.getRcaReason()) ? rca.getRcaReason() : "");
                hashtable.put("otherReasonDetail", StringUtils.isNotBlank(rca.getOtherReasonDetail()) ? rca.getOtherReasonDetail() : "");

                hashtable.put("failedChangeCriteria", StringUtils.isNotBlank(rca.getFailedChangeCriteria()) ? rca.getFailedChangeCriteria() : "");
                hashtable.put("fcil", StringUtils.isNotBlank(rca.getFailedChangeImpactLevel()) ? rca.getFailedChangeImpactLevel() : "");
                hashtable.put("failedChangeAssignee", StringUtils.isNotBlank(rca.getFailedChangeAssignee()) ? rca.getFailedChangeAssignee() : "");
                hashtable.put("failedChangeAssigneeGroup", StringUtils.isNotBlank(rca.getFailedChangeAssigneeGroup()) ? rca.getFailedChangeAssigneeGroup() : "");
                hashtable.put("sourceOfNotification", StringUtils.isNotBlank(rca.getSourceNotification()) ? rca.getSourceNotification() : "");
                hashtable.put("rootCauseDesc", StringUtils.isNotBlank(rca.getRcaEventDetail().getExecutiveSummary()) ? rca.getRcaEventDetail().getExecutiveSummary() : "");

                hashtable.put("fiveW_1", StringUtils.isNotBlank(rca.getRcaEventDetail().getWhyProblem()) ? rca.getRcaEventDetail().getWhyProblem() : "");
                hashtable.put("fiveW_2", StringUtils.isNotBlank(rca.getRcaEventDetail().getWhy1()) ? rca.getRcaEventDetail().getWhy1() : "");
                hashtable.put("fiveW_3", StringUtils.isNotBlank(rca.getRcaEventDetail().getWhy2()) ? rca.getRcaEventDetail().getWhy2() : "");
                hashtable.put("fiveW_4", StringUtils.isNotBlank(rca.getRcaEventDetail().getWhy3()) ? rca.getRcaEventDetail().getWhy3() : "");
                hashtable.put("fiveW_5", StringUtils.isNotBlank(rca.getRcaEventDetail().getWhy4()) ? rca.getRcaEventDetail().getWhy4() : "");

                hashtable.put("futPrevent", StringUtils.isNotBlank(rca.getRcaPreventionDetail().getFuturePrevention()) ? rca.getRcaPreventionDetail().getFuturePrevention() : "");
                hashtable.put("newPolnProcTxt", StringUtils.isNotBlank(rca.getRcaPreventionDetail().getNewPoliciesImplemented()) ? rca.getRcaPreventionDetail().getNewPoliciesImplemented() : "");
                hashtable.put("approvalsReviews", StringUtils.isNotBlank(rca.getApprovalsOrReviews()) ? rca.getApprovalsOrReviews() : "");
                hashtable.put("notesEscalations", StringUtils.isNotBlank(rca.getNotesAndEscalations()) ? rca.getNotesAndEscalations() : "");


                hashtable.put("atnt_internal", StringUtils.isNotBlank(rca.getPrimaryRcaCause().getServiceProvider()) ? rca.getPrimaryRcaCause().getServiceProvider() : "");
                hashtable.put("otheratnt_internal", StringUtils.isNotBlank(rca.getPrimaryRcaCause().getOtherServiceProvider()) ? rca.getPrimaryRcaCause().getOtherServiceProvider() : "");
                hashtable.put("rcaFactorDC", StringUtils.isNotBlank(rca.getPrimaryRcaCause().getLocationOfBusinessImpact()) ? rca.getPrimaryRcaCause().getLocationOfBusinessImpact() : "");
                hashtable.put("otherrcaFactorDC", StringUtils.isNotBlank(rca.getPrimaryRcaCause().getOtherLocOfBusinessImpact()) ? rca.getPrimaryRcaCause().getOtherLocOfBusinessImpact() : "");

                hashtable.put("rootCause", StringUtils.isNotBlank(rca.getPrimaryRcaCause().getRootOrContributingCause()) ? rca.getPrimaryRcaCause().getRootOrContributingCause() : "");
                hashtable.put("causeCategory", StringUtils.isNotBlank(rca.getPrimaryRcaCause().getCauseCategory()) ? rca.getPrimaryRcaCause().getCauseCategory() : "");
                hashtable.put("causeSubcategory", StringUtils.isNotBlank(rca.getPrimaryRcaCause().getCauseSubCategory()) ? rca.getPrimaryRcaCause().getCauseSubCategory() : "");
                hashtable.put("ciCategory", StringUtils.isNotBlank(rca.getPrimaryRcaCause().getOutageCategory()) ? rca.getPrimaryRcaCause().getOutageCategory() : "");
                hashtable.put("ciSubcategory", StringUtils.isNotBlank(rca.getPrimaryRcaCause().getOutageSubCategory()) ? rca.getPrimaryRcaCause().getOutageSubCategory() : "");
                hashtable.put("locSystem", StringUtils.isNotBlank(rca.getPrimaryRcaCause().getLocOfSystem()) ? rca.getPrimaryRcaCause().getLocOfSystem() : "");


                // contributing causes
                if (CollectionUtils.isNotEmpty(rca.getRcaHelper().getContributingCauses())) {
                    int i = 1;
                    for (RcaCause rcaCause : rca.getRcaHelper().getContributingCauses()) {
                        if (i <= 5) {
                            hashtable.put("contribrcaSL_internal_" + i, StringUtils.isNotBlank(rcaCause.getOutageCategory()) ? rcaCause.getOutageCategory() : "");
                            hashtable.put("contribrcaFactorDC_" + i, StringUtils.isNotBlank(rcaCause.getLocationOfBusinessImpact()) ? rcaCause.getLocationOfBusinessImpact() : "");
                            hashtable.put("contribrootCause_" + i, StringUtils.isNotBlank(rcaCause.getRootOrContributingCause()) ? rcaCause.getRootOrContributingCause() : "");
                            hashtable.put("contribcauseCategory_" + i, StringUtils.isNotBlank(rcaCause.getCauseCategory()) ? rcaCause.getCauseCategory() : "");
                            hashtable.put("contribcauseSubcategory_" + i, StringUtils.isNotBlank(rcaCause.getCauseSubCategory()) ? rcaCause.getCauseSubCategory() : "");
                            hashtable.put("contribciCategory_" + i, StringUtils.isNotBlank(rcaCause.getOutageSubCategory()) ? rcaCause.getOutageCategory() : "");
                            hashtable.put("contribciSubcategory_" + i, StringUtils.isNotBlank(rcaCause.getOutageSubCategory()) ? rcaCause.getOutageSubCategory() : "");
                            hashtable.put("contribcauseAdditionalDetail_" + i, StringUtils.isNotBlank(rcaCause.getCauseSummary()) ? rcaCause.getCauseSummary() : "");

                        }
                        i++;
                    }
                }
                // actions
               /* if (CollectionUtils.isNotEmpty(rca.getRcaActions())) {
                    int i = 1;
                    for (RcaAction rcaAction : rca.getRcaActions()) {
                        if (i <= 5) {
                            hashtable.put("a" + i + "_" + "actionDesc", StringUtils.isNotBlank(rcaAction.getActionDesc()) ? rcaAction.getActionDesc() : "");
                            hashtable.put("a" + i + "_" + "actionOwner", StringUtils.isNotBlank(rcaAction.getAssignedTo()) ? rcaAction.getAssignedTo() : "");
                            hashtable.put("a" + i + "_" + "targetDate", rcaAction.getTargetDate() != null ? CommonUtil.convertDateToStringFormat(rcaAction.getTargetDate()) : "");
                            hashtable.put("a" + i + "_" + "actionStatus", StringUtils.isNotBlank(rcaAction.getActionStatus()) ? rcaAction.getActionStatus() : "");
                        }
                        i++;
                    }
                }*/

                index++;
            } catch (Exception e) {
                log.error("Error filling RCA Coordinator hashtable:  ");
                log.error(e.getMessage(), e);
            }

            hashtable.put("count", String.valueOf(index));
            Hashtable<String, String> responses = hashtable;
            Hashtable<String, String[]> profileResponses = null;
            Hashtable<String, String[]> businessEntitiesResponses = null;

            Date sTime = new Date();
            writeAccountIntoReport(responses, profileResponses, businessEntitiesResponses);
        }
        log.debug("complete writeReport().");


    }

    private String getRcaEditors(Rca rca) {
        List<String> rcaEditorsNames = new ArrayList<String>();
        if (CollectionUtils.isNotEmpty(rca.getRcaEditors())) {
            for (RcaEditor rcaEditor : rca.getRcaEditors()) {
                if (rcaEditor.getGpsUser() != null) {
                    rcaEditorsNames.add(BluePages.getNotesIdFromIntranetId(rcaEditor.getGpsUser().getEmail()));
                }
            }

        }
        return StringUtils.join(rcaEditorsNames, ",");
    }

    public byte[] generateActionSummaryReport(List<RcaAction> actionList, String path) {
        log.debug("generateActionSummaryReport()..........");
        try {
            String templateName = path + "ActionSummary.xls";
            log.debug("reading xls template: " + templateName);
            File xlsTemplate = new File(templateName);
            log.debug("creating workbook.....");
            template = Workbook.getWorkbook(xlsTemplate);
            log.debug("Filling properties in a hashtable.......");

            log.debug("Prepare excel workbooks.......");
            prepareExcelWorkbooks();
            createTemplateMappings("ActionSummary");
            writeReportForActionSummary(actionList);
            writeReportToBytes();

        } catch (BiffException e) {
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        log.debug("end generateAnalysisReport()");
        return finishedReport;

    }

    private void writeReportForActionSummary(List<RcaAction> actionList) {

        log.debug("writeReportForRcaSummary().......");
        //int rowsUsed = 0;
        //TODO: setup the 3 excel workbooks here to have their sheets in a Hashtable to get to them easily
        templateSheets = new Hashtable<Integer, Sheet>();
        reportSheets = new Hashtable<Integer, WritableSheet>();
        writableTemplateSheets = new Hashtable<Integer, WritableSheet>();
        sheetCounters = new Hashtable<String, Integer>();
        log.debug("Starting template, report and writable template cache");
        Date tcS = new Date();
        for (int i = 0; i < templateData.size(); i++) {
            PageTemplate pt = templateData.get(i);
            int rowStart = pt.getStartRow();
            int colStart = pt.getStartCol();
            int rowEnd = pt.getEndRow();
            int colEnd = pt.getEndCol();

            if (!(rowStart == -1 || colStart == -1 || rowEnd == -1 || colEnd == -1)) {
                //only cache the sheets that we can modify in the template
                templateSheets.put(i, template.getSheet(i));
                reportSheets.put(i, report.getSheet(i));
                writableTemplateSheets.put(i, templateWriteable.getSheet(i));
                //TODO: Put sheet numbers here
                sheetCounters.put("" + i, 1);
                log.debug("Cached page " + i + " pt.pageIndex = " + pt.getPageIndex());
            }
        }


        Date tcE = new Date();
        log.debug("Finished template caching, " + (tcE.getTime() - tcS.getTime()) + " ms");
        // put number of reports to be generated here
        for (RcaAction action : actionList) {
            int index = 1;
            hashtable = new Hashtable<String, String>();
            try {

                // Action Data sheet values
                hashtable.put("accountid", String.valueOf(action.getRca().getContract().getContractId()));
                hashtable.put("accountName", action.getRca().getContract().getTitle());
                hashtable.put("actionItemNo", String.valueOf(action.getActionNumber()));
                hashtable.put("parentRCAId", String.valueOf(action.getRca().getRcaNumber()));
                hashtable.put("primaryTicket", StringUtils.isNotBlank(action.getRca().getProblemIndidentRecord()) ? action.getRca().getProblemIndidentRecord() : "");
                hashtable.put("assignedBy", action.getRcaActionHelper().getActionCreatedBy() != null ? action.getRcaActionHelper().getActionCreatedBy().getNotesId() : "");
                hashtable.put("assignedTo", action.getAssignedTo() != null ? BluePages.getNotesIdFromIntranetId(action.getAssignedTo()) : "");
                hashtable.put("updatedBy", action.getRcaActionHelper().getActionUpdatedBy() != null ? action.getRcaActionHelper().getActionUpdatedBy().getNotesId() : "");
                hashtable.put("actionStatus", action.getActionStatus() != null ? action.getActionStatus() : "");
                hashtable.put("percentageComplete", action.getCompletePercent() != null ? String.valueOf(action.getCompletePercent()) + "%" : "");
                hashtable.put("assignedOn", action.getAssignedOn() != null ? CommonUtil.convertDateToStringFormat(action.getAssignedOn()) : "");
                hashtable.put("targetDate", action.getTargetDate() != null ? CommonUtil.convertDateToStringFormat(action.getTargetDate()) : "");
                hashtable.put("actionCloseDate", action.getCloseDate() != null ? CommonUtil.convertDateToStringFormat(action.getCloseDate()) : "");
                hashtable.put("reasonRCARequired", StringUtils.isNotBlank(action.getRca().getRcaReason()) ? action.getRca().getRcaReason() : "");
                hashtable.put("actionTxt", StringUtils.isNotBlank(action.getActionDesc()) ? action.getActionDesc() : "");
                hashtable.put("createDate", action.getCreatedOn() != null ? CommonUtil.convertDateToStringFormat(action.getCreatedOn()) : "");
                hashtable.put("additionalInfo", StringUtils.isNotBlank(action.getAdditionalInfo()) ? action.getAdditionalInfo() : "");
                hashtable.put("actionNotes", StringUtils.isNotBlank(action.getActionItemNote()) ? action.getActionItemNote() : "");
                hashtable.put("rcaNotes", StringUtils.isNotBlank(action.getRcaNote()) ? action.getRcaNote() : "");
                hashtable.put("resolutionDescTAHiddenField", StringUtils.isNotBlank(action.getRcaActionHelper().getResolutionDescription()) ? action.getRcaActionHelper().getResolutionDescription() : "");

                index++;
            } catch (Exception e) {
                log.error("Error filling Action summary hashtable:  ");
                log.error(e.getMessage(), e);
            }

            hashtable.put("count", String.valueOf(index));
            Hashtable<String, String> responses = hashtable;
            Hashtable<String, String[]> profileResponses = null;
            Hashtable<String, String[]> businessEntitiesResponses = null;

            Date sTime = new Date();
            writeAccountIntoReport(responses, profileResponses, businessEntitiesResponses);
        }
        log.debug("complete writeReport().");


    }

    public byte[] generateCustomerFormattedReport(List<Rca> rcaList, String path) {
        log.debug("generateCustomerFormattedReport()..........");
        try {
            String templateName = path + "CustomerFormatted.xls";
            log.debug("reading xls template: " + templateName);
            File xlsTemplate = new File(templateName);
            log.debug("creating workbook.....");

            template = Workbook.getWorkbook(xlsTemplate);
            Path xlsPath = Paths.get(templateName);
            byte[] data = Files.readAllBytes(xlsPath);
            // template = getWorkbookFromBytes(data);
            log.debug("Filling properties in a hashtable.......");

            log.debug("Prepare excel workbooks.......");
            prepareExcelWorkbooksCR();
            //createTemplateMappings("CustomerFormatted");
            createTemplateMappingsCR();
            writeReportForCR(rcaList);
            writeReportToBytes();

        } catch (BiffException e) {
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        log.debug("end generateCustomerFormattedReport()");

        return finishedReport;


    }

    public byte[] generateRcaDetailedReport(List<Rca> rcaList, String path) {
        log.debug("generateRcaDetailedReport()..........");
        try {
            String templateName = path + "RcaDetailed.xls";
            log.debug("reading xls template: " + templateName);
            File xlsTemplate = new File(templateName);
            log.debug("creating workbook.....");

            template = Workbook.getWorkbook(xlsTemplate);
            Path xlsPath = Paths.get(templateName);
            byte[] data = Files.readAllBytes(xlsPath);
            log.debug("Filling properties in a hashtable.......");

            log.debug("Prepare excel workbooks.......");
            prepareExcelWorkbooksCR();

            createTemplateMappingsCR();
            writeReportForCR(rcaList);
            writeReportToBytes();

        } catch (BiffException e) {
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        log.debug("end generateAnalysisReport()");

        return finishedReport;


    }


    private void writeReportForCR(List<Rca> rcaList) {
    	log.debug("writeReportForCR().... " +rcaList.size());
        //int rowsUsed = 0;
        int totalCompleted = 0;
        //TODO: setup the 3 excel workbooks here to have their sheets in a Hashtable to get to them easily
        templateSheets = new Hashtable<Integer, Sheet>();
        reportSheets = new Hashtable<Integer, WritableSheet>();
        writableTemplateSheets = new Hashtable<Integer, WritableSheet>();
        sheetCounters = new Hashtable<String, Integer>();
//        log.debug("Starting template, report and writable template cache");
        Date tcS = new Date();
        for (int i = 0; i < templateData.size(); i++) {
            PageTemplate pt = templateData.get(i);
            int rowStart = pt.getStartRow();
            int colStart = pt.getStartCol();
            int rowEnd = pt.getEndRow();
            int colEnd = pt.getEndCol();

            if (!(rowStart == -1 || colStart == -1 || rowEnd == -1 || colEnd == -1)) {
                //only cache the sheets that we can modify in the template
                templateSheets.put(i, template.getSheet(i));
                reportSheets.put(i, report.getSheet(i));
                writableTemplateSheets.put(i, templateWriteable.getSheet(i));
                sheetCounters.put("" + i, rowStart != rowEnd ? rowEnd : 0);
//                log.debug("Cached page " + i + " pt.pageIndex = " + pt.getPageIndex());
            }
        }

        Date tcE = new Date();
        log.debug("writeReportForCR(): Finished template caching, " + (tcE.getTime() - tcS.getTime()) + " ms");
        for (Rca rca : rcaList) {
            Hashtable<String, String[]> crHashtable = new Hashtable<String, String[]>();
            try {

                // RCA Data sheet values
                crHashtable.put("&table$accountid$", convertStringToStringArray(String.valueOf(rca.getContract().getContractId())));
                crHashtable.put("&table$accountId$", convertStringToStringArray(String.valueOf(rca.getContract().getContractId())));
                crHashtable.put("$accountId$", convertStringToStringArray(String.valueOf(rca.getContract().getContractId())));
                crHashtable.put("&table$accountname$", convertStringToStringArray(String.valueOf(rca.getContract().getTitle())));
                crHashtable.put("rcaId", convertStringToStringArray(String.valueOf(rca.getRcaNumber())));
                crHashtable.put("RCAtitle", convertStringToStringArray(String.valueOf(rca.getTitle())));
                crHashtable.put("associatedToolPrimary", convertStringToStringArray(rca.getPrimaryTicketAssociation()));
                crHashtable.put("primaryTicket", convertStringToStringArray(rca.getProblemIndidentRecord()));
                crHashtable.put("severityIssue", rca.getSeverity() != null ? convertStringToStringArray(String.valueOf(rca.getSeverity())) : convertStringToStringArray(""));
                crHashtable.put("primaryTicketType", StringUtils.isNotBlank(rca.getPrimaryTicketType()) ? convertStringToStringArray(rca.getPrimaryTicketType()) : convertStringToStringArray(""));
                crHashtable.put("associatedWith", convertStringToStringArray(rca.getRcaAssociatedWith()));
                crHashtable.put("rcaAssociatedWith", crHashtable.get("associatedWith"));
                crHashtable.put("reasonRCARequired", convertStringToStringArray(rca.getRcaReason()));
                crHashtable.put("outageStartDate", convertStringToStringArray(CommonUtil.convertDateToStringFormat(rca.getIncidentStartTime())));
                crHashtable.put("outageEndDate", convertStringToStringArray(CommonUtil.convertDateToStringFormat(rca.getIncidentEntTime())));
                crHashtable.put("outageDuration", convertStringToStringArray(CommonUtil.calculateTimestampDifference(rca.getIncidentStartTime(), rca.getIncidentEntTime())));
                crHashtable.put("rcaCreationDate", rca.getCreateDate() != null ? convertStringToStringArray(CommonUtil.convertDateToStringFormat(rca.getCreateDate())) : convertStringToStringArray(""));
                crHashtable.put("rcaEndDate", rca.getDueDate() != null ? convertStringToStringArray(CommonUtil.convertDateToStringFormat(rca.getDueDate())) : convertStringToStringArray(""));
                crHashtable.put("rcaApprovedDate", rca.getRcaHelper().getApprovedDate() != null ? convertStringToStringArray(CommonUtil.convertDateToStringFormat(rca.getRcaHelper().getApprovedDate())) : convertStringToStringArray(""));
                crHashtable.put("rcaCloDate", rca.getCloseDate() != null ? convertStringToStringArray(CommonUtil.convertDateToStringFormat(rca.getCloseDate())) : convertStringToStringArray(""));
                crHashtable.put("rcaStatus", convertStringToStringArray(rca.getRcaStage()));
                crHashtable.put("listSLASLO", convertStringToStringArray(rca.getListOfSlaSloImpacted()));
                crHashtable.put("ticketNumber_Inc_*", convertStringToStringArray(rca.getRcaHelper().getSecondaryProblemIncidentRecord()));
                crHashtable.put("secondaryTicket", convertStringToStringArray(rca.getRcaHelper().getSecondaryProblemIncidentRecord()));
                crHashtable.put("associatedTool_Inc_*", convertStringToStringArray(rca.getRcaHelper().getSecondaryAssociatedTool()));
                crHashtable.put("secondaryAssociatedTool", convertStringToStringArray(rca.getRcaHelper().getSecondaryAssociatedTool()));

                crHashtable.put("ownersList", convertStringToStringArray(StringUtils.isNotBlank(rca.getRcaOwner()) ? BluePages.getNotesIdFromIntranetId(rca.getRcaOwner()) : ""));
                crHashtable.put("RCACoordname", convertStringToStringArray(rca.getRcaStage()));
                crHashtable.put("delegateList", convertStringToStringArray(StringUtils.isNotBlank(rca.getRcaDelegate()) ? BluePages.getNotesIdFromIntranetId(rca.getRcaDelegate()) : ""));
                crHashtable.put("rcaCoordManager", convertStringToStringArray(StringUtils.isNotBlank(rca.getRcaHelper().getRcaCoordinatorManager()) ? rca.getRcaHelper().getRcaCoordinatorManager() : ""));
                crHashtable.put("dpeList", convertStringToStringArray(rca.getRcaHelper().getRcaDpe() != null ? rca.getRcaHelper().getRcaDpe().getEmail() : ""));
                crHashtable.put("dpeNotesId", convertStringToStringArray(rca.getRcaHelper().getRcaDpe() != null ? rca.getRcaHelper().getRcaDpe().getNotesId() : ""));
                crHashtable.put("additionalEditorList", convertStringToStringArray(getRcaEditors(rca)));
                crHashtable.put("numOfActions", convertStringToStringArray(CollectionUtils.isNotEmpty(rca.getRcaActions()) ? String.valueOf(rca.getRcaActions().size()) : "0"));
                crHashtable.put("actionsOpened", convertStringToStringArray(String.valueOf(rca.getActionItemOpen() != null ? rca.getActionItemOpen() : "0")));
                crHashtable.put("actionsClosed", convertStringToStringArray(String.valueOf(rca.getActionItemClosed() != null ? rca.getActionItemOpen() : "0")));


                crHashtable.put("custImpact", convertStringToStringArray(StringUtils.isBlank(rca.getCustomerImpacted()) || rca.getCustomerImpacted().equalsIgnoreCase("N") ? "No" : "Yes"));
                crHashtable.put("othercustomerImpacted", convertStringToStringArray(StringUtils.isNotBlank(rca.getCustomerOther()) ? rca.getCustomerOther() : ""));
                crHashtable.put("managedBy", convertStringToStringArray(rca.getManagedBy()));
                crHashtable.put("impactCust", convertStringToStringArray(rca.getCustomerImpactedList()));
                crHashtable.put("practImpacted", convertStringToStringArray(rca.getTotalGpsPractitionersImpacted()));
                crHashtable.put("OS", convertStringToStringArray(rca.getRcaEventDetail().getImpactedTower()));
                crHashtable.put("keyIssues", convertStringToStringArray(rca.getImpactDetails()));
                crHashtable.put("failingComponentModel", convertStringToStringArray(rca.getBusinessImpactInMins()));
                crHashtable.put("locImpact", convertStringToStringArray(rca.getLocationOfFailure()));
                crHashtable.put("serviceImpacted", convertStringToStringArray(rca.getServiceImpacted()));
                crHashtable.put("systemServerName", convertStringToStringArray(rca.getSystemImpacted()));
                crHashtable.put("repeatInci", convertStringToStringArray(loadRepeatIssue(rca.getRcaEventDetail().getRepeatIssue())));
                crHashtable.put("sourceOfNoti", convertStringToStringArray(rca.getSourceNotification()));
                crHashtable.put("futureRisk", convertStringToStringArray(rca.getRcaEventDetail().getRepeatRisk()));

                crHashtable.put("inciDetail", convertStringToStringArray(rca.getRcaEventDetail().getIssueDescription()));
                crHashtable.put("servRestore", convertStringToStringArray(rca.getRcaEventDetail().getHowServiceRestored()));
                crHashtable.put("chronOfEvents", StringUtils.isNotBlank(rca.getRcaEventDetail().getChronology()) ? convertStringToStringArray(rca.getRcaEventDetail().getChronology()) : convertStringToStringArray(""));

                crHashtable.put("fiveW_1", convertStringToStringArray(rca.getRcaEventDetail().getWhyProblem()));
                crHashtable.put("fiveW_2", convertStringToStringArray(rca.getRcaEventDetail().getWhy1()));
                crHashtable.put("fiveW_3", convertStringToStringArray(rca.getRcaEventDetail().getWhy2()));
                crHashtable.put("fiveW_4", convertStringToStringArray(rca.getRcaEventDetail().getWhy3()));
                crHashtable.put("fiveW_5", convertStringToStringArray(rca.getRcaEventDetail().getWhy4()));

                crHashtable.put("rootCauseDesc", convertStringToStringArray(rca.getRcaEventDetail().getExecutiveSummary()));

                crHashtable.put("newPolnProc", convertStringToStringArray(com.gps.util.StringUtils.isBlank(rca.getRcaPreventionDetail().getNewPoliciesImplemented())
                        || rca.getRcaPreventionDetail().getNewPoliciesImplemented().equalsIgnoreCase("N") ? "No" : "Yes"));
                crHashtable.put("newPolnProcTxt", convertStringToStringArray(rca.getRcaPreventionDetail().getComments()));
                crHashtable.put("futPrevent", convertStringToStringArray(rca.getRcaPreventionDetail().getFuturePrevention()));


                //primary rca cause
                crHashtable.put("atnt_internal", convertStringToStringArray(rca.getPrimaryRcaCause().getServiceProvider()));
                crHashtable.put("otheratnt_internal", convertStringToStringArray(rca.getPrimaryRcaCause().getOtherServiceProvider()));
                crHashtable.put("rcaSL", convertStringToStringArray(rca.getPrimaryRcaCause().getOutageCategory()));
                crHashtable.put("rcaSL_internal", convertStringToStringArray(rca.getPrimaryRcaCause().getOutageCategory()));
                crHashtable.put("rcaFactorDC", convertStringToStringArray(rca.getPrimaryRcaCause().getLocationOfBusinessImpact()));
                crHashtable.put("otherrcaFactorDC", convertStringToStringArray(rca.getPrimaryRcaCause().getOtherLocOfBusinessImpact()));
                crHashtable.put("rootCause", convertStringToStringArray(rca.getPrimaryRcaCause().getRootOrContributingCause()));
                crHashtable.put("causeCategory", convertStringToStringArray(rca.getPrimaryRcaCause().getCauseCategory()));
                crHashtable.put("causeSubcategory", convertStringToStringArray(rca.getPrimaryRcaCause().getCauseSubCategory()));
                crHashtable.put("ciCategory", convertStringToStringArray(rca.getPrimaryRcaCause().getOutageSubCategory()));
                crHashtable.put("ciSubcategory", convertStringToStringArray(rca.getPrimaryRcaCause().getOutageSubCategory2()));

                crHashtable.put("othercustomerImpacted", convertStringToStringArray(rca.getCustomerOther()));
                // contributing rca causes
                loadContributingCausesForCF(rca, crHashtable);

                actionResponses = new ArrayList<Hashtable<String, String[]>>();
                if (CollectionUtils.isNotEmpty(rca.getRcaActions())) {
                    for (RcaAction rcaAction : rca.getRcaActions()) {
                        Hashtable<String, String[]> crActions = new Hashtable<String, String[]>();
                        crActions.put("actionItemNo", convertStringToStringArray(rcaAction.getActionNumber()));
                        crActions.put("actionTxt", convertStringToStringArray(rcaAction.getActionDesc()));
                        crActions.put("assignedTo", convertStringToStringArray(rcaAction.getAssignedTo()));
                        crActions.put("targetDate", convertStringToStringArray(CommonUtil.convertDateToStringFormat(rcaAction.getTargetDate())));
                        crActions.put("actionStatus", convertStringToStringArray(rcaAction.getActionStatus()));
                        crActions.put("actionOwner", convertStringToStringArray(com.gps.util.StringUtils.isNotBlank(rcaAction.getAssignedTo()) ? BluePages.getNotesIdFromIntranetId(rcaAction.getAssignedTo()) : ""));
                        crActions.put("isOpened", convertStringToStringArray(rcaAction.getActionStatus().equalsIgnoreCase("open") ? "true" : "false"));
                        actionResponses.add(crActions);
                    }
                }

            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

            Hashtable<String, String[]> responses = crHashtable;
            Hashtable<String, String[]> profileResponses = null;
            Hashtable<String, String[]> businessEntitiesResponses = null;


            if (responses.size() > 0) {
                log.debug("Account " + totalCompleted + " started");
                Date sTime = new Date();

                writeAccountIntoReportCR(responses, profileResponses, businessEntitiesResponses, getRcaNumbers(rcaList));
                Date eTime = new Date();
//                log.debug("Account " + totalCompleted + " time : " + (eTime.getTime() - sTime.getTime()) + " ms");
            }

            totalCompleted++;


        }
        
    }


    private String loadRepeatIssue(String repeatIssue) {
        if (com.gps.util.StringUtils.isNotBlank(repeatIssue)) {
            if (repeatIssue.equalsIgnoreCase("Repeat")) {
                return "Repeat--Occurred at least twice before";
            }
            if (repeatIssue.equalsIgnoreCase("Yes")) {
                return "Yes--occurred once before";
            }
            if (repeatIssue.equalsIgnoreCase("No")) {
                return "No--Has not occurred";
            }
        }
        return "";
    }

    private void loadContributingCausesForCF(Rca rca, Hashtable<String, String[]> crHashtable) {
        if (CollectionUtils.isNotEmpty(rca.getRcaHelper().getContributingCauses())) {
            int i = 1;
            for (RcaCause rcaCause : rca.getRcaHelper().getContributingCauses()) {
                if (i <= 4) {
                    crHashtable.put("contribrcaSL_internal_" + i, convertStringToStringArray(rcaCause.getOutageCategory()));
                    crHashtable.put("contribrcaFactorDC_" + i, convertStringToStringArray(rcaCause.getLocationOfBusinessImpact()));
                    crHashtable.put("contribotherrcaFactorDC_" + i, convertStringToStringArray(rcaCause.getOtherLocOfBusinessImpact()));
                    crHashtable.put("contribrootCause_" + i, convertStringToStringArray(rcaCause.getRootOrContributingCause()));
                    crHashtable.put("contribcauseCategory_" + i, convertStringToStringArray(rcaCause.getCauseCategory()));
                    crHashtable.put("contribcauseSubcategory_" + i, convertStringToStringArray(rcaCause.getCauseSubCategory()));
                    crHashtable.put("contribciCategory_" + i, convertStringToStringArray(rcaCause.getOutageSubCategory()));
                    crHashtable.put("contribciSubcategory_" + i, convertStringToStringArray(rcaCause.getOutageSubCategory2()));
                    crHashtable.put("contribcauseAdditionalDetail_" + i, convertStringToStringArray(rcaCause.getCauseSummary()));
                    i++;
                }
            }
        }
    }

    private ArrayList<String> getRcaNumbers(List<Rca> rcaList) {
        ArrayList<String> rcaNumbers = new ArrayList<String>();
        for (Rca rca : rcaList) {
            rcaNumbers.add(rca.getRcaNumber());
        }
        return rcaNumbers;
    }

    protected void createTemplateMappingsCR() {
        if (template == null) {
            return;
        }
        //if no ^tableStart^ is found, we look for ^rowEnd^ and use that to determine
        //the row that will be copied
        //$colorMaps$, etc. will be outside the ^tableEnd^ but before a ^rowEnd^ col (in the row only case)
        for (int sheetCount = 0; sheetCount < template.getNumberOfSheets(); sheetCount++) {
            Sheet currSheet = template.getSheet(sheetCount);
            PageTemplate pt = new PageTemplate();

            int rowStart = -1;
            Cell tableStart = findTableStart(currSheet);
            Cell rowEnd = null;
            if (tableStart == null) {
                rowEnd = findRowEnd(currSheet);
                if (rowEnd != null) {
                    rowStart = rowEnd.getRow();
                }
                if (rowStart == -1) {
                    templateData.add(pt); //add a blank page template
                    continue; //skip this iteration
                }
            } else {//only if sheet has ^tableStart^
                rowStart = tableStart.getRow() + 1;
                if (rowStart > 0) {
                    pt.setStartCell(tableStart.getRow() + 1, tableStart.getColumn());
                    //Find wildcard tag in the same row as ^tableStart^
                    Cell[] tableStartRow = currSheet.getRow(tableStart.getRow());
                    for (int j = 0; j < tableStartRow.length; j++) {
                        Cell tsrCell = tableStartRow[j];
                        String contents = tsrCell.getContents();
                        try {
                            if (contents.toLowerCase().startsWith("^wildcard^")) {
                                handleWildCard(tsrCell, currSheet);
                            }
                        } catch (Exception e) {
                            log.error("Template Error (sheet = " + currSheet.getName() + " r=" + tsrCell.getRow() + " c=" + tsrCell.getColumn() + ") : " + contents);
                            log.error(com.gps.util.StringUtils.getStackTraceAsString(e));
                        }
                    }
                } else {
                    templateData.add(pt); //add a blank page template
                    continue; //skip this iteration
                }
            }

            //now we loop until a ^rowEnd^ or a ^tableEnd^ or run out of rows
            boolean tableEnd = false;
            for (int i = rowStart; i < currSheet.getRows(); i++) {
                Cell[] currRow = currSheet.getRow(i);
                for (int j = 0; j < currRow.length; j++) {
                    Cell tCell = currRow[j];
                    String contents = tCell.getContents();
                    try {
                        if (contents.equalsIgnoreCase("^tableStart^")) {
                            pt.setStartCell(tCell.getRow(), tCell.getColumn());
                        }
                        if (contents.equalsIgnoreCase("^tableEnd^")) {
                            //we don't need to do anymore the template data has been copied out
                            pt.setEndCell(tCell.getRow(), tCell.getColumn());
                            tableEnd = true;
                        } else if (contents.equalsIgnoreCase("^rowEnd^")) {
                            pt.setStartCell(tCell.getRow(), 0);
                            pt.setEndCell(tCell.getRow(), tCell.getColumn());
                            pt.setRowEndUsed(true);
                        } else if (contents.startsWith("^") && tableEnd == false) {
                            //look for template specific variables that we need to pull out
                            //like ^map, ^color*, ^accountFilter
                            if (contents.toLowerCase().startsWith("^map::")) {
                                handleMap(pt, tCell, currSheet);
                            } else if (contents.toLowerCase().startsWith("^concatcell::")) {
                                handleConcatCell(pt, tCell, currSheet);
                            } else if (contents.toLowerCase().startsWith("^colorcols^")) {
                                handleColorMap(pt, tCell, currSheet);
                            } else if (contents.toLowerCase().startsWith("^accountfilter^")) {
                                handleAccountFilter(tCell, currSheet);
                            } else if (contents.toLowerCase().startsWith("^wildcard^")) {//Find wildCard tag in rows below ^tableStart^
                                handleWildCard(tCell, currSheet);
                            } else if (contents.toLowerCase().startsWith("^actionrow^")) {//Find actionRow tag in rows below ^tableStart^
                                actionRow = tCell.getRow();
                            } else if (contents.toLowerCase().startsWith("^incidentrow^")) {//Find actionRow tag in rows below ^tableStart^
                                incidentRow = tCell.getRow();
                            }
                        } else if (!contents.equalsIgnoreCase("^tableStart^") &&
                                !contents.equalsIgnoreCase("") && tableStart != null &&
                                tCell.getColumn() == tableStart.getColumn()) {
                            handleRowFilter(pt, tCell);
                        }
                    } catch (Exception e) {
                        log.error("Template Error (sheet = " + currSheet.getName() + " r=" + tCell.getRow() + " c=" + tCell.getColumn() + ") : " + contents);
                        log.error(com.gps.util.StringUtils.getStackTraceAsString(e));
                    }
                }
            }
            pt.setPageIndex(templateData.size());
            templateData.add(pt);
        }

    }

    private void prepareExcelWorkbooksCR() {
        baos = new ByteArrayOutputStream();
        ByteArrayOutputStream tBaos = new ByteArrayOutputStream();
        try {
            report = Workbook.createWorkbook(baos, template);
        } catch (IOException e) {
            log.error(com.gps.util.StringUtils.getStackTraceAsString(e));
        }
        try {
            templateWriteable = Workbook.createWorkbook(tBaos, template);
        } catch (IOException e) {
            log.error(com.gps.util.StringUtils.getStackTraceAsString(e));
        }

    }

    private void writeReportForCustomerFormatted(List<Rca> rcaList) {
        log.debug("writeReportForRcaSummary().......");
        //int rowsUsed = 0;
        //TODO: setup the 3 excel workbooks here to have their sheets in a Hashtable to get to them easily
        templateSheets = new Hashtable<Integer, Sheet>();
        reportSheets = new Hashtable<Integer, WritableSheet>();
        writableTemplateSheets = new Hashtable<Integer, WritableSheet>();
        sheetCounters = new Hashtable<String, Integer>();
        log.debug("Starting template, report and writable template cache");
        Date tcS = new Date();
        for (int i = 0; i < templateData.size(); i++) {
            PageTemplate pt = templateData.get(i);
            int rowStart = pt.getStartRow();
            int colStart = pt.getStartCol();
            int rowEnd = pt.getEndRow();
            int colEnd = pt.getEndCol();

            if (!(rowStart == -1 || colStart == -1 || rowEnd == -1 || colEnd == -1)) {
                //only cache the sheets that we can modify in the template
                templateSheets.put(i, template.getSheet(i));
                reportSheets.put(i, report.getSheet(i));
                writableTemplateSheets.put(i, templateWriteable.getSheet(i));
                //TODO: Put sheet numbers here
                sheetCounters.put("" + i, 1);
                log.debug("Cached page " + i + " pt.pageIndex = " + pt.getPageIndex());
            }
        }


        Date tcE = new Date();
        log.debug("Finished template caching, " + (tcE.getTime() - tcS.getTime()) + " ms");
        // put number of reports to be generated here
        for (Rca rca : rcaList) {
            int index = 1;
            hashtable = new Hashtable<String, String>();
            try {

                // RCA Data sheet values

                //hashtable.put("ciorcaId", String.valueOf(rca.getContract().getContractId()));
                //hashtable.put("applicationName", rca.getContract().getTitle());
                hashtable.put("rcaNumber", String.valueOf(rca.getRcaNumber()));


                index++;
            } catch (Exception e) {
                log.error("Error filling RCA Coordinator hashtable:  ");
                log.error(e.getMessage(), e);
            }

            hashtable.put("count", String.valueOf(index));
            Hashtable<String, String> responses = hashtable;
            Hashtable<String, String[]> profileResponses = null;
            Hashtable<String, String[]> businessEntitiesResponses = null;

            Date sTime = new Date();
            writeAccountIntoReport(responses, profileResponses, businessEntitiesResponses);
        }
        log.debug("complete writeReport().");
    }


    private Workbook getWorkbookFromBytes(byte[] wbData) {
        Workbook wkbook = null;

        try {
            wkbook = Workbook.getWorkbook(new ByteArrayInputStream(wbData));
        } catch (BiffException be) {
            log.error(com.gps.util.StringUtils.getStackTraceAsString(be));
            wkbook = null;
        } catch (IOException ioe) {
            log.error(com.gps.util.StringUtils.getStackTraceAsString(ioe));
            wkbook = null;
        }

        return wkbook;
    }

    private byte[] getFileBytes(File file) {
        FileInputStream fileInputStream = null;

        byte[] bFile = new byte[(int) file.length()];

        try {
            //convert file into array of bytes
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(bFile);
            fileInputStream.close();

            for (int i = 0; i < bFile.length; i++) {
                System.out.print((char) bFile[i]);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return bFile;
    }


    private void handleColorMap(PageTemplate pt, Cell colorCol, Sheet s) {
        //get the next 3 cells and make sure the cells in order are:
        //^colorCols^, ^colorFunc^, ^colorMap^ and ^colorValues^
        int cRow = colorCol.getRow();
        int cCol = colorCol.getColumn();
        Cell colorFuncs = s.getCell(cCol + 1, cRow);
        Cell colorMap = s.getCell(cCol + 2, cRow);
        Cell colorValues = s.getCell(cCol + 3, cRow);
        if (!colorFuncs.getContents().equalsIgnoreCase("^colorFunc^") ||
                !colorMap.getContents().equalsIgnoreCase("^colorMap^") ||
                !colorValues.getContents().equalsIgnoreCase("^colorValues^")) {
            return; //all the required values aren't there
        } else {
            // go down row by row until we reach empty cells
            int rowOffset = 1;
            String colorContents = s.getCell(cCol, cRow + rowOffset).getContents();
            String funcContents = s.getCell(cCol + 1, cRow + rowOffset).getContents();
            String mapContents = s.getCell(cCol + 2, cRow + rowOffset).getContents();
            String valContents = s.getCell(cCol + 3, cRow + rowOffset).getContents();

            while (!colorContents.equalsIgnoreCase("") &&
                    !funcContents.equalsIgnoreCase("") &&
                    !mapContents.equalsIgnoreCase("") &&
                    !valContents.equalsIgnoreCase("") &&
                    (cRow + rowOffset) < s.getRows()) {
                pt.setColorMap(cRow + rowOffset, com.gps.util.StringUtils.parseCSVLine(colorContents),
                        com.gps.util.StringUtils.parseCSVLine(funcContents),
                        com.gps.util.StringUtils.parseCSVLine(mapContents),
                        com.gps.util.StringUtils.parseCSVLine(valContents));
                rowOffset++;
                if (cRow + rowOffset < s.getRows()) {
                    colorContents = s.getCell(cCol, cRow + rowOffset).getContents();
                    funcContents = s.getCell(cCol + 1, cRow + rowOffset).getContents();
                    mapContents = s.getCell(cCol + 2, cRow + rowOffset).getContents();
                    valContents = s.getCell(cCol + 3, cRow + rowOffset).getContents();
                }
            }
        }
    }


    private String[] convertStringToStringArray(String s) {
        if (s == null) {
            s = "";
        }
        String[] ar = new String[1];
        ar[0] = s;
        return ar;
    }


    private void writeAccountIntoReportCR(Hashtable<String, String[]> responses, Hashtable<String, String[]> profileResponses, Hashtable<String, String[]> businessEntitiesResponses, ArrayList<String> rcaNumberList) {
        try {
//            log.info("inside writeAccountIntoReport");
            int currRowsUsed = 0;
            //int pageCount = 0;
            int addVal = 0;
            int rowsToAdd = 0;
            int currPage = 0;
            int oldVal = 0;
            //write the account that is selected into the report.. assuming
            //it passes the template account filter
            if (useTemplateFilter && templateAccountFilter != null) {
                //we need to evaluate the templateAccountFilter
                //if the filter is true we use the account
                if (!templateAccountFilter.trim().equalsIgnoreCase("")) {
                    String response = handleJBeans(templateAccountFilter, responses, profileResponses, null, businessEntitiesResponses);
                    if (!response.equalsIgnoreCase("true")) {
                        return;
                    }
                }
            }

      /*Enumeration<String> enumKey = responses.keys();
      while(enumKey.hasMoreElements()) {
          String key = enumKey.nextElement();
          String[] val = responses.get(key);
          logger.info("key: "+key+" ; value: "+val[0]);
      }*/

            if (responses.containsKey("dpeList")) {
                String[] dpeName = responses.get("dpeList");
                dpeName[0] = BluePages.getNotesIdFromIntranetId(dpeName[0]);
                responses.remove("dpeList");
                log.info("dpeName: " + dpeName[0]);
                responses.put("dpeList", dpeName);
            }

            String sheetHash = "";
            for (currPage = 0; currPage < templateData.size(); currPage++) {
                Date pSTime = new Date();
                PageTemplate pt = templateData.get(currPage);
                //int slaIndex = 0;
                String associatedWith = "";
                if (responses.get("associatedWith") != null) {
                    associatedWith = responses.get("associatedWith")[0].toString();
                }
                String[] associatedWiths = associatedWith.split(",");
                ArrayList<String> slaList = new ArrayList<String>();
                ArrayList<String> sloList = new ArrayList<String>();
                ArrayList<String> miList = new ArrayList<String>();
                for (int assoc = 0; assoc < associatedWiths.length; assoc++) {
                    if (associatedWiths[assoc].contains("SLA")) {
                        slaList.add(associatedWiths[assoc]);
                    } else if (associatedWiths[assoc].contains("SLO")) {
                        sloList.add(associatedWiths[assoc]);
                    } else if (associatedWiths[assoc].contains("MI")) {
                        miList.add(associatedWiths[assoc]);
                    }
                }
                int rowStart = pt.getStartRow();
                int colStart = pt.getStartCol();
                int rowEnd = pt.getEndRow();
                int colEnd = pt.getEndCol();

                if (rowStart == -1 || colStart == -1 || rowEnd == -1 || colEnd == -1) {
                    Date pETime = new Date();
                    log.debug("  Page " + currPage + " (skipped) time " + (pETime.getTime() - pSTime.getTime()) + " ms");
                    continue; //skip this page since there is nothing on it.
                }

                //TODO: make these read the data from a hashtable
                Sheet currTemplateSheet = templateSheets.get(currPage);//template.getSheet(currPage);
                WritableSheet currReportSheet = reportSheets.get(currPage);//report.getSheet(currPage);
                WritableSheet currTemplateWSheet = writableTemplateSheets.get(currPage);//templateWriteable.getSheet(currPage);
                Range[] mergedCells = currTemplateWSheet.getMergedCells();
                if (rowStart != rowEnd) {
                    for (int x = 0; x < rowEnd; x++) {
                        try {
                            currReportSheet.setRowView(x, false);
                        } catch (RowsExceededException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
                int rowOffset = /*pt.getStartRow() +*/ /*rowsUsed*/ /* (pt.getEndRow() - pt.getStartRow())*/ 0;
                if (sheetCounters.containsKey("" + currPage))
                    rowOffset = oldVal = ((Integer) sheetCounters.get("" + currPage)).intValue();
                sheetHash = currTemplateSheet.getName() + "";
                //pageCount ++;
                for (int currRow = rowStart; currRow <= rowEnd; currRow++) {
                    //check for the row filter
                    String rowFilter = pt.getRowFilter(currRow);
                    boolean doRow = true;
                    //test the row filter
                    if (rowFilter != null && includeGreen == false) {
                        String result = handleJBeans(rowFilter, responses, profileResponses, pt, businessEntitiesResponses);
                        if (!result.equalsIgnoreCase("true")) {
                            doRow = false;
                        }
                    }
                    if (doRow == false) { //since we aren't using a row, need to fix the row offset
                        //clear the row
                        for (int clR = 0; clR < currReportSheet.getColumns(); clR++) {
                            //TODO: the calculation of the row needs to be changed to account for tables
                            jxl.write.Label tL = null;
                            tL = new jxl.write.Label(clR, currRow + rowOffset, "");
                            try {
                                currReportSheet.addCell(tL);
                            } catch (RowsExceededException e) {
                                log.error(com.gps.util.StringUtils.getStackTraceAsString(e));
                            } catch (WriteException e) {
                                log.error(com.gps.util.StringUtils.getStackTraceAsString(e));
                            }
                        }
                        if (wildcards.containsKey(sheetHash))
                            wildcards.get(sheetHash).wildKeyIndex++;
                        rowOffset--;
                    }
                    if (doRow) {
                        boolean wildRowProcessed = false;
                        addVal = 1;
                        String key = "";
                        Integer wildRownNum = new Integer(-1);
                        if (wildcards.containsKey(sheetHash)) {
                            if (wildcards.get(sheetHash).wildKeyIndex < wildcards.get(sheetHash).wildKeys.size()) {
                                key = wildcards.get(sheetHash).wildKeys.get(wildcards.get(sheetHash).wildKeyIndex);
                                wildRownNum = wildcards.get(sheetHash).wildCardCounts.get(key);
                            }
                        }
                        if (currRow == wildRownNum.intValue()) {
                            try {
                                if (key.contains("XXX")) {
                                    rowsToAdd = slaList.size();
                                } else if (key.contains("YYY")) {
                                    rowsToAdd = sloList.size();
                                } else if (key.contains("ZZZ")) {
                                    rowsToAdd = miList.size();
                                } else {
                                    rowsToAdd = Integer.parseInt(handleDollarSignCR(key, responses));
                                }
                            } catch (NumberFormatException e) {
                                rowsToAdd = 1;
                            }
                            wildRowProcessed = true;
                        } else if (currRow == actionRow) {
                            if (responses.get("numOfActions") != null) {
                                String numOfActions = responses.get("numOfActions")[0];
                                if (StringUtils.isNotBlank(numOfActions)) {
                                    rowsToAdd = Integer.parseInt(numOfActions);
                                }
                            }
                        } else if (currRow == incidentRow) {
                            //rowsToAdd = getIncidentsList("pRCA_Inc_", responses);
                            //rowsToAdd = 0;
                        } else {
                            rowsToAdd = 1;
                        }
                        for (int cRow = 1; cRow <= rowsToAdd; cRow++) {
                            //int rowHeight = 1;
                            for (int currCol = colStart; currCol <= colEnd; currCol++) {
                                String newContents = "";
                                // get cell from template and then copy to report
                                Cell ct = currTemplateSheet.getCell(currCol,
                                        currRow);
                                // right here, we need to get the cell format data
                                // from the template cell to the target cell
                                WritableCell source = currTemplateWSheet
                                        .getWritableCell(currCol, currRow); // currReportSheet.getWritableCell(currCol,currRow);
                                WritableCell target = source.copyTo(currCol,
                                        rowOffset + currRow);// currReportSheet.getWritableCell(currCol,rowOffset+currRow);//source.copyTo(currCol,rowOffset+currRow);
                                if (source.getCellFormat() != null) {
                                    try {
                                        WritableCellFormat sourceCellFormat = new WritableCellFormat(source.getCellFormat());
                                        sourceCellFormat.setWrap(true);
                                        target.setCellFormat(sourceCellFormat);
                                    } catch (Exception e) {
                                        log.error(e.getMessage(), e);
                                    }

                                }

                                // now based on the contents of the ct, determine
                                // what to do w/ the target
                                String contents = ct.getContents();

                                if (rowFilter != null && currCol == 0) {
                                    // remove the rowFilter from being shown
                                    contents = "";
                                }
                                if (contents.startsWith("&find")) {
                                    String displayItem = contents.substring(contents.indexOf("!") + 1, contents.indexOf("$"));
                                    ArrayList<String> rcaIDs = rcaNumberList;
                                    for (int rcaid = 0; rcaid < rcaIDs.size(); rcaid++) {
                                        Hashtable<String, String[]> rcaBlob = null;
                                        //  rcaBlob = getRCABlobData(rcaIDs.get(rcaid));
                                        rcaBlob = responses;
                                        String associatedWithFromRCA = "";
                                        if (rcaBlob.get("associatedWith") != null && !rcaBlob.get("associatedWith")[0].equals("")) {
                                            associatedWithFromRCA = rcaBlob.get("associatedWith")[0].toString();
                                            String findVal = handleAsterick(contents.substring(contents.indexOf("$"), contents.length()),
                                                    responses, cRow);
                                            if (!findVal.equals("") && associatedWithFromRCA.contains(findVal)) {
                                                if (rcaBlob.get(displayItem) != null) {
                                                    String displayVal = rcaBlob.get(displayItem)[0];
                                                    newContents = newContents.equalsIgnoreCase("") ? displayVal : newContents + "\n" + displayVal;
                                                }
                                            }

                                        }
                                    }
                                } else if (contents.startsWith("&rca")) {
                                    String rcaID = responses.get("parentRCAIssueId")[0];
                                    Hashtable<String, String[]> rcaBlob = null;
                                    //rcaBlob = getRCABlobData(rcaID);
                                    rcaBlob = responses;
                                    newContents = handleDollarSignCR(contents.substring(contents.indexOf("$"), contents.length()),
                                            responses);
                                } else if (contents.startsWith("&sla") || contents.startsWith("&slo") || contents.startsWith("&mi")) {
                                    String accountID = responses.get("&table$accountid$")[0];
                                    Hashtable<String, String[]> dpescBlob = null;
                                    // dpescBlob = getBlobData(accountID);
                                    String valtoSearch = "";
                                    String tempSL = "";
                                    if (contents.contains("XXX")) {
                                        if(CollectionUtils.isNotEmpty(slaList)) {
                                            tempSL = slaList.get(cRow - 1).toString().substring(0, slaList.get(cRow - 1).toString().indexOf("-"));
                                            contents = contents.replace("XXX", tempSL);
                                            valtoSearch = slaList.get(cRow - 1);
                                        }
                                    } else if (contents.contains("YYY")) {
                                        if(CollectionUtils.isNotEmpty(sloList)) {
                                            tempSL = sloList.get(cRow - 1).toString().substring(0, sloList.get(cRow - 1).toString().indexOf("-"));
                                            contents = contents.replace("YYY", tempSL);
                                            valtoSearch = sloList.get(cRow - 1);
                                        }
                                    } else if (contents.contains("ZZZ")) {
                                        if(CollectionUtils.isNotEmpty(miList)) {
                                            tempSL = miList.get(cRow - 1).toString().substring(0, miList.get(cRow - 1).toString().indexOf("-"));
                                            contents = contents.replace("ZZZ", tempSL);
                                            valtoSearch = miList.get(cRow - 1);
                                        }
                                    }

                                    String[] temp = new String[1];
                                    String varName = "";
                                    if(dpescBlob != null && dpescBlob.size() > 0) {
                                        for (Enumeration e = dpescBlob.keys(); e.hasMoreElements(); ) {
                                            String searchkey = (String) e.nextElement();
                                            String line = dpescBlob.get(searchkey)[0];
                                            CSVReader csvr = new CSVReader(new StringReader(line));
                                            String[] parsedLine = csvr.getLine();
                                            if (parsedLine == null) {
                                                parsedLine = new String[1];
                                                parsedLine[0] = "";
                                            }
                                            try {
                                                csvr.close();
                                            } catch (IOException ex) {
                                                log.error(com.gps.util.StringUtils.getStackTraceAsString(ex));
                                            }
                                            //System.out.println("searchKey = "+ key + " Value = " + parsedLine[0]);

                                            if (searchkey.startsWith("currId" + tempSL) && parsedLine[0].equals(valtoSearch)) {
                                                varName = contents.replace("*", searchkey.substring(searchkey.length() - 1));
                                                break;
                                            }
                                        }
                                    }
                                    if (varName != null && !varName.equals("")) {
                                        varName = varName.substring(contents.indexOf("$") + 1, contents.length() - 1);
                                        temp = dpescBlob.get(varName);
                                        newContents = convertStringArrayToString(temp);
                                    }
                                } else if (contents.startsWith("&")) {
                                    newContents = handleAmpersandCR(contents,
                                            responses, profileResponses, businessEntitiesResponses);
                                } else if (contents.startsWith("$")) {
                                    if (contents.contains("+")) {
                                        //   if(typeFilter.equalsIgnoreCase("rca"))
                                        contents = contents.replace("+", convertStringArrayToString(responses.get("&table$formid$")));
                                /*    else
                                        contents = contents.replace("+", convertStringArrayToString(responses.get("&table$actioncolid$")));
                                    newContents = handleDollarSign(contents,
                                            responses); */
                                    } else if (currRow == actionRow) {
                                        if (CollectionUtils.isNotEmpty(actionResponses)) {
                                            newContents = handleDollarSignCR(contents, actionResponses.get(cRow - 1));
                                        }
                                    } else if (currRow == incidentRow) {
                                        // newContents = handleAsterick(contents, responses, Integer.parseInt((String) incidents.get(cRow-1)));
                                    } else {
                                        newContents = handleAsterick(contents, responses, cRow);
                                    }
                                } else if (contents.toLowerCase().startsWith("jbeans::")) {
                                    newContents = handleJBeans(contents, responses,
                                            profileResponses, pt, businessEntitiesResponses);
                                } else if (contents.toLowerCase().startsWith("func::")) {
                                    newContents = handleFunc(contents, responses, profileResponses, businessEntitiesResponses);
                                } else if (contents.startsWith("^")) {
                                    newContents = handleCaret(contents, responses, profileResponses, pt, businessEntitiesResponses);
                                } else {
                                    newContents = contents;
                                }
                                if (newContents.length() > EXCEL_CELL_LIMIT) {
                                    newContents = newContents.substring(0, EXCEL_CELL_LIMIT);
                                }
                                doColoring(target, ct, responses, profileResponses, pt, businessEntitiesResponses);

                                int contentRows = newContents.split("\r").length;
                                //if( contentRows > rowHeight)
                                //rowHeight = contentRows;

                                if (ct.getType() == CellType.NUMBER_FORMULA
                                        || ct.getType() == CellType.STRING_FORMULA
                                        || ct.getType() == CellType.BOOLEAN_FORMULA
                                        || ct.getType() == CellType.FORMULA_ERROR) {
                                    try {
                                        currReportSheet.addCell(target);
                                    } catch (RowsExceededException e) {
                                        log.error(com.gps.util.StringUtils.getStackTraceAsString(e));
                                    } catch (WriteException e) {
                                        log.error(com.gps.util.StringUtils.getStackTraceAsString(e));
                                    } catch (Exception e) {
                                        log.error(com.gps.util.StringUtils.getStackTraceAsString(e));
                                    }
                                } else if (isStringANumber(newContents)) {
                                    jxl.write.Number nCell = new jxl.write.Number(
                                            target.getColumn(), target.getRow(),
                                            Double.parseDouble(newContents));
                                    if (target.getCellFormat() != null)
                                        nCell.setCellFormat(target.getCellFormat());

                                    try {
                                        currReportSheet.addCell(nCell);
                                    } catch (RowsExceededException e) {
                                        log.error(com.gps.util.StringUtils.getStackTraceAsString(e));
                                    } catch (WriteException e) {
                                        log.error(com.gps.util.StringUtils.getStackTraceAsString(e));
                                    }
                                } else {
                                    jxl.write.Label lCell = new Label(target
                                            .getColumn(), target.getRow(),
                                            newContents);
                                    if (target.getCellFormat() != null)
                                        lCell.setCellFormat(target.getCellFormat());

                                    try {
                                        currReportSheet.addCell(lCell);
                                    } catch (RowsExceededException e) {
                                        log.error(com.gps.util.StringUtils.getStackTraceAsString(e));
                                    } catch (WriteException e) {
                                        log.error(com.gps.util.StringUtils.getStackTraceAsString(e));
                                    } catch (Exception e) {
                                        log.error(com.gps.util.StringUtils.getStackTraceAsString(e));
                                    }
                                }

                            }
                            for (int mLen = 0; mLen < mergedCells.length; mLen++) {
                                Range r = mergedCells[mLen];
                                Cell topLeft = r.getTopLeft();
                                Cell botRight = r.getBottomRight();
                                if (topLeft.getRow() == currRow) {
                                    try {
                                        currReportSheet.mergeCells(topLeft.getColumn(), topLeft.getRow() + rowOffset, botRight.getColumn(), botRight.getRow() + rowOffset);
                                    } catch (RowsExceededException e) {
                                        log.error(com.gps.util.StringUtils.getStackTraceAsString(e));
                                    } catch (WriteException e) {
                                        log.error(com.gps.util.StringUtils.getStackTraceAsString(e));
                                    }
                                }
                            }

                      /*try {
                    if(rowHeight>1)
    					currReportSheet.setRowView(currRow+rowOffset, (rowHeight * 13 * 500), false);
				} catch (Exception e) {
					logger.error(StringUtils.getStackTraceAsString(e));
				}*/

                            if (wildRowProcessed || currRow == actionRow || currRow == incidentRow)
                                rowOffset++;
                            currRowsUsed++;

                        }
                        if (wildRowProcessed) {
                            wildcards.get(sheetHash).wildKeyIndex++;
                            rowOffset--;
                            addVal = 0;
                        }
                        if (currRow == actionRow || currRow == incidentRow) {
                            rowOffset--;
                        }
                        // test to see if the row we are currently copying has the start of
                        // a
                        // merged cell in it. If so, merge the cells with the offsets.


                    } else
                        addVal = 0;
                }
                //clean up unused rows here.
                if (rowStart != rowEnd) {
                    //determine what the last row we wrote on was, then clean the rows that would still be shown.
                    if (rowOffset < 0) {
                        //we need to clear some rows
                        for (int i = 0; i > rowOffset; i--) {
                            //get the row @ rowEnd + i

                            for (int clR = 0; clR < currReportSheet.getColumns(); clR++) {
                                jxl.write.Label tL = null;
                                tL = new jxl.write.Label(clR, rowEnd + i, "");
                                try {
                                    currReportSheet.addCell(tL);
                                } catch (RowsExceededException e) {
                                    log.error(com.gps.util.StringUtils.getStackTraceAsString(e));
                                } catch (WriteException e) {
                                    log.error(com.gps.util.StringUtils.getStackTraceAsString(e));
                                }
                            }

                        }
                    }
                }
                Date pETime = new Date();
                log.debug("  Page " + currPage + " time : " + (pETime.getTime() - pSTime.getTime()) + " ms");
                if (rowStart == rowEnd)
                    sheetCounters.put("" + (currPage), (currRowsUsed + (oldVal - 1)) + addVal);
                else
                    sheetCounters.put("" + (currPage), (currRowsUsed + (oldVal)) + 0);
                currRowsUsed = 0;

                if (wildcards.containsKey(sheetHash))
                    wildcards.get(sheetHash).wildKeyIndex = 0;
            }

            //sheetCounters.put(""+(currPageVal), (currRowsUsed + (oldVal-1)) + addVal);
            //return (currRowsUsed - pageCount) + addVal;
        }catch (Exception e){
            log.error(e.getMessage(),e);
        }
    }


    private String handleJBeans(String content,
                                Hashtable<String, String[]> responses,
                                Hashtable<String, String[]> profileResponses,
                                PageTemplate pt,
                                Hashtable<String, String[]> businessEntitiesResponses) {
        //remove the jbeans:: from the statement to process
        if (content.toLowerCase().startsWith("jbeans::")) {
            content = content.substring(8, content.length());
        }
        //String contentBefore = content;
        try {
            content = runInternalFunctions(content, responses, profileResponses, pt, businessEntitiesResponses);
        } catch (Exception e) {
            return "[ERROR]";
        }
        Object bshEval = null;
        try {
            bshEval = bsh.eval(content);
        } catch (ParseException e) {
            //logger.error(StringUtils.getStackTraceAsString(e));
            //logger.info(StringUtils.getStackTraceAsString(e));
            bshEval = "false";
            //System.out.println(contentBefore);
        } catch (EvalError e) {
            //logger.error(StringUtils.getStackTraceAsString(e));
            //logger.info(StringUtils.getStackTraceAsString(e));
            bshEval = "false";
        }
        String temp = "";
        if (bshEval != null) {
            temp = bshEval.toString();
        }

        //TODO: not quite sure why this is used, so I'm commenting it out.
    /*
    Object bshGet = null;
    try {
      bshGet = bsh.get("out");
    } catch (EvalError e) {
      logger.error(StringUtils.getStackTraceAsString(e));
    }

    String intVal = "";
    if (bshGet != null) {
      intVal = bshGet.toString();
    }*/
        temp = com.gps.util.StringUtils.fixNullToEmptyString(temp).trim();

        return temp;
    }

    /**
     * @return the String with all the functions completed.
     */
    private String runInternalFunctions(String function, Hashtable<String, String[]> responses, Hashtable<String, String[]> profileResponses, PageTemplate pt,
                                        Hashtable<String, String[]> businessEntitiesResponses) {
        int ptr = 0;
        int endFcn = 0;
        int startFcn = 0;
        String[] keys = {"f.var", "^map", "f.div"};
        for (int k = 0; k < keys.length; k++) {
            ptr = function.indexOf(keys[k]);
            while (ptr != -1) {
                String innerFcn = function.substring(function.indexOf(keys[k]) + keys[k].length(), function.length());
                String findResult = "";
                if (innerFcn.startsWith("(") && keys[k].contains("var")) {
                    startFcn = innerFcn.indexOf("(", 0);
                    endFcn = innerFcn.indexOf(")", startFcn + 1);
                    findResult = "";
                    String varInFind = innerFcn.substring(startFcn + 1, endFcn);
                    findResult = "";
                    if (varInFind.startsWith("$")) {
                        findResult = handleDollarSignCR(varInFind, responses);
                    } else if (varInFind.startsWith("&")) {
                        findResult = handleAmpersandCR(varInFind, responses, profileResponses, businessEntitiesResponses);
                    } else {
                        findResult = "";
                    }
                    findResult = cleanUpStringResult(findResult);
                    boolean findResultIsNumber = isStringANumber(findResult);
                    if (ptr > 0) {
                        if (findResultIsNumber)
                            function = function.substring(0, ptr) + findResult.trim() + function.substring(function.indexOf(")", ptr) + 1, function.length());
                        else
                            function = function.substring(0, ptr) + "\"" + findResult.trim() + "\"" + function.substring(function.indexOf(")", ptr) + 1, function.length());
                    } else {
                        if (findResultIsNumber)
                            function = findResult.trim() + function.substring(function.indexOf(")", ptr) + 1, function.length());
                        else
                            function = "\"" + findResult.trim() + "\"" + function.substring(function.indexOf(")", ptr) + 1, function.length());
                    }
                } else if (innerFcn.startsWith("(") && keys[k].contains("div")) {
                    startFcn = innerFcn.indexOf("(", 0);
                    endFcn = innerFcn.indexOf(")", startFcn + 1);
                    findResult = "";
                    String varInFind = innerFcn.substring(startFcn + 1, endFcn);

                    String quotedNum = varInFind.trim().substring(0, varInFind.trim().lastIndexOf(",")).trim();
                    Pattern p2 = Pattern.compile("[^0-9\\.\\-]+");
                    Matcher m = p2.matcher(quotedNum);
                    StringBuffer sb = new StringBuffer();
                    boolean result = m.find();
                    while (result) {
                        m.appendReplacement(sb, "");
                        result = m.find();
                    }
                    m.appendTail(sb);

                    String cleanQuotedNum = sb.toString();
                    Double tflt = Double.valueOf(cleanQuotedNum);
                    Double divnum = Double.valueOf(varInFind.trim().substring(varInFind.trim().lastIndexOf(",") + 1, varInFind.trim().length()));
                    if (divnum != 0) {
                        findResult = Double.toString(tflt / divnum);
                    }

                    boolean findResultIsNumber = isStringANumber(findResult);
                    if (ptr > 0) {
                        if (findResultIsNumber)
                            function = function.substring(0, ptr) + findResult.trim() + function.substring(function.indexOf(")", ptr) + 1, function.length());
                        else
                            function = function.substring(0, ptr) + "\"" + findResult.trim() + "\"" + function.substring(function.indexOf(")", ptr) + 1, function.length());
                    } else {
                        if (findResultIsNumber)
                            function = findResult.trim() + function.substring(function.indexOf(")", ptr) + 1, function.length());
                        else
                            function = "\"" + findResult.trim() + "\"" + function.substring(function.indexOf(")", ptr) + 1, function.length());
                    }
                } else if (innerFcn.startsWith("^")) { //the ^map function
                    findResult = "";
                    int startVar = innerFcn.indexOf("(");
                    int endVar = innerFcn.indexOf(")", startVar + 1);
                    String map = innerFcn.substring(1, startVar);
                    String key = innerFcn.substring(startVar + 1, endVar);

                    if (key.startsWith("\"") && key.endsWith("\"")) {
                        key = key.substring(1, key.length() - 1);
                    }
                    if (key.startsWith("$") && key.endsWith("$")) {
                        key = key.substring(1, key.length() - 1);
                        if (responses.containsKey(key)) {
                            key = responses.get(key)[0];
                        } else if (profileResponses.containsKey(key)) {
                            key = profileResponses.get(key)[0];
                        } else {
                            key = "";
                        }
                    }

                    Hashtable<String, String> temp = pt.getHashMap(map);
                    findResult = temp.get(key);
                    if (findResult == null)
                        findResult = "";
                    findResult = cleanUpStringResult(findResult);
                    boolean findResultIsNumber = isStringANumber(findResult);
                    if (ptr > 0) {
                        if (findResultIsNumber)
                            function = function.substring(0, ptr) + findResult.trim() + function.substring(function.indexOf(")", ptr) + 1, function.length());
                        else
                            function = function.substring(0, ptr) + "\"" + findResult.trim() + "\"" + function.substring(function.indexOf(")", ptr) + 1, function.length());
                    } else {
                        if (findResultIsNumber)
                            function = findResult.trim() + function.substring(function.indexOf(")", ptr) + 1, function.length());
                        else
                            function = "\"" + findResult.trim() + "\"" + function.substring(function.indexOf(")", ptr) + 1, function.length());
                    }
                } else {
                    log.error(innerFcn + " is an unknown function");
                }
                ptr = function.indexOf(keys[k], ptr + findResult.length());
            }
        }
        return function;
    }


    private String handleDollarSignCR(String contents, Hashtable<String, String[]> responses) {
        contents = contents.substring(1, contents.length() - 1);
        String[] cResponse;
        if (contents.equalsIgnoreCase("_timestamp_")) {
            if (!timeStampPrinted)
                timeStampPrinted = true;
            else
                return "";
        }
        log.info("in handle dollar sign");
        cResponse = responses.get(contents);
        String cResp = "";
        if (cResponse != null) {
            cResp = convertStringArrayToString(cResponse);
        }
        log.info("cResp :" + cResp);
        return cResp;
    }


    private String handleAmpersandCR(String contents,
                                     Hashtable<String, String[]> responses,
                                     Hashtable<String, String[]> profileResponses,
                                     Hashtable<String, String[]> businessEntitiesResponses) {

        String newContent = "[unknown]";
        try {
            String table = contents.substring(1, contents.indexOf("$"));
            String varname = contents.substring(contents.indexOf("$") + 1, contents.length() - 1);

            if (table.equalsIgnoreCase("prf") && profileResponses != null) {
                String[] temp = profileResponses.get(varname);
                newContent = convertStringArrayToString(temp);
            } else if (table.equalsIgnoreCase("table")) {
                String[] temp = responses.get(contents.toLowerCase());
                newContent = convertStringArrayToString(temp);
            } else if (table.equalsIgnoreCase("businessentities")) {
                String[] temp = businessEntitiesResponses.get(varname);
                newContent = convertStringArrayToString(temp);
            } else if (table.startsWith("status")) {
                String referData = convertStringArrayToString(responses.get("REFER"));
                String Pn$Sn = contents.substring(8, contents.length() - 1);
                if (Pn$Sn.contains("$")) {
                    String Pn = Pn$Sn.substring(0, Pn$Sn.indexOf("$"));
                    String Sn = Pn$Sn.substring(Pn$Sn.indexOf("$") + 1);
                }
                //newContent = convertStringArrayToString(responses.get(contents.toLowerCase()));
            }
        } catch (Exception e) {
            log.error("Error in handleAmpersand: " + contents);
            log.error(com.gps.util.StringUtils.getStackTraceAsString(e));
            newContent = "[ERROR]";
        }
        return newContent;
    }

    private String handleAsterick(String contents, Hashtable<String, String[]> responses, int cRow) {
        contents = contents.substring(1, contents.length() - 1);
        contents = contents.replace("*", cRow + "");
        String[] cResponse = responses.get(contents);
        String cResp = "";
        if (cResponse != null) {
            cResp = convertStringArrayToString(cResponse);
        }

        return cResp;
    }


    private String handleFunc(String content,
                              Hashtable<String, String[]> responses,
                              Hashtable<String, String[]> profileResponses,
                              Hashtable<String, String[]> businessEntitiesResponses) {
        content = content.substring(6, content.length());
        return handleFuncBody(content, responses, profileResponses, businessEntitiesResponses);

    }

    private String handleFuncBody(String content,
                                  Hashtable<String, String[]> responses,
                                  Hashtable<String, String[]> profileResponses,
                                  Hashtable<String, String[]> businessEntitiesResponses) {
        String returnVal = "";
        try {
            if (content.startsWith("val")) {
                String varFull = content.substring(content.indexOf("(") + 1, content.lastIndexOf(")"));
                if (varFull.startsWith("$")) {
                    returnVal = handleDollarSignCR(varFull, responses);
                } else if (varFull.startsWith("&")) {
                    returnVal = handleAmpersandCR(varFull, responses, profileResponses, businessEntitiesResponses);
                } else {
                    returnVal = "";
                }
                returnVal = cleanUpStringResult(returnVal);
                returnVal = reformat(returnVal);
            } else if (content.startsWith("trim")) {
                String varFull = content.substring(content.indexOf("(") + 1, content.lastIndexOf(")"));
                if (varFull.startsWith("$")) {
                    returnVal = handleDollarSignCR(varFull, responses);
                } else if (varFull.startsWith("&")) {
                    returnVal = handleAmpersandCR(varFull, responses, profileResponses, businessEntitiesResponses);
                } else {
                    returnVal = "";
                }
                returnVal = cleanUpStringResult(returnVal);
                returnVal = returnVal.trim();
            } else if (content.startsWith("concat")) {
                StringBuffer sb = new StringBuffer();
                String args = content.substring(content.indexOf("(") + 1, content.lastIndexOf(")"));
                //variable which needs to be concatenated for the wildcard
                String function = content.substring(0, content.indexOf("("));
                String contentElement = args.substring(0, args.indexOf(","));
                //the count upto which index will be appended to the variable
                String countVar = args.substring(args.indexOf(",") + 1);
                countVar = handleDollarSignCR(countVar, responses);
                if (!countVar.equals("")) {
                    int count = Integer.parseInt(countVar);
                    if (function.equalsIgnoreCase("concat")) {//concatenate with serial number
                        for (int i = 1; i <= count; i++) {
                            String var = contentElement.replace("*", Integer.toString(i));
                            sb.append(i + ". " + handleDollarSignCR(var, responses));
                            if (i < count) {
                                sb.append("\r\n");
                            }
                        }
                    } else if (function.equalsIgnoreCase("concatB")) {//concatenate with blank line
                        for (int i = 1; i <= count; i++) {
                            String var = contentElement.replace("*", Integer.toString(i));
                            sb.append(handleDollarSignCR(var, responses));
                            if (i < count) {
                                sb.append("\r\n\r\n");
                            }
                        }
                    } else if (function.equalsIgnoreCase("concatC")) {//concatenate with blank line
                        for (int i = 1; i <= count; i++) {
                            String var = contentElement.replace("*", Integer.toString(i));
                            sb.append(handleDollarSignCR(var, responses));
                            if (sb.length() > 0 && i < count) {
                                sb.append(", ");
                            }
                        }
                    }
                }
                returnVal = sb.toString();
            } else if (content.startsWith("OR")) {
                String body = content.substring(content.indexOf("(") + 1, content.lastIndexOf(")"));
                String arg1 = body.substring(0, body.indexOf(","));
                String arg2 = body.substring(body.indexOf(",") + 1);
                if (arg1.startsWith("$")) {
                    arg1 = arg1.substring(1, arg1.length() - 1);
                    String[] cResponse = responses.get(arg1);
                    String cResp = "";
                    if (cResponse != null) {
                        cResp = convertStringArrayToString(cResponse);
                        returnVal = cResp;
                    }
                    if (cResponse == null || cResp.equals("")) {
                        returnVal = handleFuncBody(arg2, responses, profileResponses, businessEntitiesResponses);
                    }
                }
            }
        } catch (Exception e) {
            log.error(com.gps.util.StringUtils.getStackTraceAsString(e));
            returnVal = "[ERROR]";
        }
        return returnVal;
    }

    private String handleCaret(String contents, Hashtable<String, String[]> responses, Hashtable<String, String[]> profileResponses, PageTemplate pt,
                               Hashtable<String, String[]> businessEntitiesResponses) {
        String returnVal = "";

        if (contents.startsWith("^map")) {
            try {
                String temp = contents.substring(4, contents.length());
                String mapName = temp.substring(temp.indexOf("^") + 1, temp.indexOf("("));
                String varName = temp.substring(temp.indexOf("(") + 1, temp.indexOf(")"));
                if (varName.startsWith("$")) {
                    varName = handleDollarSignCR(varName, responses);
                } else if (varName.startsWith("&")) {
                    varName = handleAmpersandCR(varName, responses, profileResponses, businessEntitiesResponses);
                }
                Hashtable<String, String> map = pt.getHashMap(mapName);
                if (map == null) {
                    returnVal = "UNKNOWN MAP: " + map;
                } else {
                    returnVal = map.get(varName);
                    if (returnVal == null) {
                        returnVal = "";
                    }
                }
            } catch (Exception e) {
                log.error("Error in handleCaret : " + contents);
                log.error(com.gps.util.StringUtils.getStackTraceAsString(e));
                returnVal = "[ERROR]";
            }
        } else if (contents.startsWith("^concatCell")) {
            try {
                String temp = contents.substring(11, contents.length());
                String concatName = temp.substring(temp.indexOf("^") + 1, temp.indexOf("("));
                ArrayList<String> questions = pt.getConcatCellQuestions(concatName);
                ArrayList<String> answers = pt.getConcatCellAnswers(concatName);

                StringBuffer concatContents = new StringBuffer();
                for (int i = 0; i < answers.size(); i++) {
                    String tAns = answers.get(i);
                    String ans = "";
                    if (tAns.startsWith("$")) {
                        ans = handleDollarSignCR(tAns, responses);
                    } else if (tAns.startsWith("func::")) {
                        ans = handleFunc(tAns, responses, profileResponses, businessEntitiesResponses);
                    } else if (tAns.startsWith("jbeans::")) {
                        ans = handleJBeans(tAns, responses, profileResponses, pt, businessEntitiesResponses);
                        ;
                    }

                    if (ans != null && !ans.equalsIgnoreCase("")) {
                        if (concatContents.length() > 0) {
                            concatContents.append("\r\n");
                        }
                        concatContents.append(questions.get(i));
                        concatContents.append(" : ");
                        concatContents.append(ans);
                    }
                }
                returnVal = concatContents.toString();
            } catch (Exception e) {
                log.error("Error in handleCaret : " + contents);
                log.error(com.gps.util.StringUtils.getStackTraceAsString(e));
                returnVal = "[ERROR]";
            }
        } else if (contents.startsWith("^concatNZCell")) {
            try {
                String temp = contents.substring(13, contents.length());
                String concatName = temp.substring(temp.indexOf("^") + 1, temp.indexOf("("));
                ArrayList<String> questions = pt.getConcatCellQuestions(concatName);
                ArrayList<String> answers = pt.getConcatCellAnswers(concatName);

                StringBuffer concatContents = new StringBuffer();
                for (int i = 0; i < answers.size(); i++) {
                    String tAns = answers.get(i);
                    String ans = "";
                    if (tAns.startsWith("$")) {
                        ans = handleDollarSignCR(tAns, responses);
                    } else if (tAns.startsWith("func::")) {
                        ans = handleFunc(tAns, responses, profileResponses, businessEntitiesResponses);
                    }
                    if (ans != null && !ans.equalsIgnoreCase("") && !ans.equalsIgnoreCase("0")) {
                        if (concatContents.length() > 0) {
                            concatContents.append("\r\n");
                        }
                        concatContents.append(questions.get(i));
                        concatContents.append(" : ");
                        concatContents.append(ans);
                    }
                }
                returnVal = concatContents.toString();
            } catch (Exception e) {
                log.error("Error in handleCaret : " + contents);
                log.error(com.gps.util.StringUtils.getStackTraceAsString(e));
                returnVal = "[ERROR]";
            }
        }
        return returnVal;
    }


    private void doColoring(WritableCell t, Cell c, Hashtable<String, String[]> responses,
                            Hashtable<String, String[]> profileResponses,
                            PageTemplate pt,
                            Hashtable<String, String[]> businessEntitiesResponses) {
        int row = c.getRow();
        int col = c.getColumn();
        boolean readAllRows = true;
        for (int i = pt.getFirstMapRow(); (i <= pt.getLastMapRow() && readAllRows); i++) {
            if (!pt.isRowEndUsed()) {
                i = row;
                readAllRows = false;
            }
            String[] colorCols = pt.getColorCols(i);
            //see if col is in the colorCols
            int idx = -1;
            if ((idx = stringArrayContains(colorCols, col + "")) != -1) {
                //get the other cols and see about coloring the column
                String colorFunc = pt.getColorFunctions(i)[idx];
                String[] colorMap = pt.getColorMaps(i);
                String[] colorValues = pt.getColorValues(i);
                //eval the colorFunc
                String result = handleJBeans(colorFunc, responses, profileResponses, pt, businessEntitiesResponses);

                //see if the result matches something in colorMap
                int idx2 = stringArrayContains(colorMap, result);
                //take the index of item in colormap to get color from colorvalues
                if (idx2 != -1) {
                    String cellColor = colorValues[idx2];
                    WritableCellFormat wfmt = null;
                    jxl.format.CellFormat cf = c.getCellFormat();
                    if (cf != null) {
                        wfmt = new WritableCellFormat(cf);
                    } else {
                        wfmt = new WritableCellFormat();
                    }
                    try {
                        if (cellColor.equalsIgnoreCase("red")) {
                            wfmt.setBackground(jxl.format.Colour.RED);
                        } else if (cellColor.equalsIgnoreCase("amber") || cellColor.equalsIgnoreCase("Yellow")) {
                            wfmt.setBackground(jxl.format.Colour.YELLOW);
                        } else if (cellColor.equalsIgnoreCase("green")) {
                            wfmt.setBackground(jxl.format.Colour.GREEN);
                        } else if (cellColor.equalsIgnoreCase("black")) {
                            wfmt.setBackground(jxl.format.Colour.BLACK);
                        } else if (cellColor.equalsIgnoreCase("blue")) {
                            wfmt.setBackground(jxl.format.Colour.BLUE);
                        }
                    } catch (WriteException e) {
                        log.error("Error in doColoring");
                        log.error(com.gps.util.StringUtils.getStackTraceAsString(e));
                    }
                    //write the cell format back into the cell.
                    t.setCellFormat(wfmt);
                }
            }
        }
    }


    public byte[] generateCFReport(List<Rca> rcaList, String path) {
        log.debug("generateRcaSummaryReport()..........");
        try {
            String templateName = path + "CustomerFormatted.xls";
            log.debug("reading xls template: " + templateName);
            File xlsTemplate = new File(templateName);
            log.debug("creating workbook.....");
            template = Workbook.getWorkbook(xlsTemplate);
            log.debug("Filling properties in a hashtable.......");

            log.debug("Prepare excel workbooks.......");
            prepareExcelWorkbooks();
            createTemplateMappings("CustomerFormatted");
            writeReportForCF(rcaList);
            writeReportToBytes();

        } catch (BiffException e) {
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        log.debug("end generateAnalysisReport()");
        return finishedReport;
    }

    private void writeReportForCF(List<Rca> rcaList) {
        log.debug("writeReportForRcaSummary().......");
        //int rowsUsed = 0;
        //TODO: setup the 3 excel workbooks here to have their sheets in a Hashtable to get to them easily
        templateSheets = new Hashtable<Integer, Sheet>();
        reportSheets = new Hashtable<Integer, WritableSheet>();
        writableTemplateSheets = new Hashtable<Integer, WritableSheet>();
        sheetCounters = new Hashtable<String, Integer>();
        log.debug("Starting template, report and writable template cache");
        Date tcS = new Date();
        for (int i = 0; i < templateData.size(); i++) {
            PageTemplate pt = templateData.get(i);
            int rowStart = pt.getStartRow();
            int colStart = pt.getStartCol();
            int rowEnd = pt.getEndRow();
            int colEnd = pt.getEndCol();

            if (!(rowStart == -1 || colStart == -1 || rowEnd == -1 || colEnd == -1)) {
                //only cache the sheets that we can modify in the template
                templateSheets.put(i, template.getSheet(i));
                reportSheets.put(i, report.getSheet(i));
                writableTemplateSheets.put(i, templateWriteable.getSheet(i));
                //TODO: Put sheet numbers here
                sheetCounters.put("" + i, rowStart != rowEnd ? rowEnd : 0);
                log.debug("Cached page " + i + " pt.pageIndex = " + pt.getPageIndex());
            }
        }


        Date tcE = new Date();
        log.debug("Finished template caching, " + (tcE.getTime() - tcS.getTime()) + " ms");
        // put number of reports to be generated here
        for (Rca rca : rcaList) {
            int index = 1;
            hashtable = new Hashtable<String, String>();
            try {

                // RCA Data sheet values

                hashtable.put("ciorcaId", String.valueOf(rca.getContract().getContractId()));
                //hashtable.put("applicationName", rca.getContract().getTitle());


                index++;
            } catch (Exception e) {
                log.error("Error filling RCA Coordinator hashtable:  ");
                log.error(e.getMessage(), e);
            }

            hashtable.put("count", String.valueOf(index));
            Hashtable<String, String> responses = hashtable;
            Hashtable<String, String[]> profileResponses = null;
            Hashtable<String, String[]> businessEntitiesResponses = null;

            Date sTime = new Date();
            writeAccountIntoReport(responses, profileResponses, businessEntitiesResponses);
        }
        log.debug("complete writeReport().");

    }

    public byte[] generateWeeklyOperationsReport(List<Rca> rcaList, String path) {

        try {
            String templateName = path + "WeeklyOperations.xls";
            log.debug("reading xls template: " + templateName);
            File xlsTemplate = new File(templateName);
            log.debug("creating workbook.....");

            template = Workbook.getWorkbook(xlsTemplate);
            Path xlsPath = Paths.get(templateName);
            byte[] data = Files.readAllBytes(xlsPath);
            log.debug("Filling properties in a hashtable.......");

            log.debug("Prepare excel workbooks.......");
            prepareExcelWorkbooksCR();

            createTemplateMappingsCR();
            writeReportForCR(rcaList);
            writeReportToBytes();

        } catch (BiffException e) {
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        log.debug("end generateAnalysisReport()");

        return finishedReport;


    }
    public byte[] generateRcaGreenTemplateReport(List<Rca> rcaList, String path) {

        try {
            String templateName = path + "RcaGreenTemplate.xls";
            log.debug("reading xls template: " + templateName);
            File xlsTemplate = new File(templateName);
            log.debug("creating workbook.....");

            template = Workbook.getWorkbook(xlsTemplate);
            Path xlsPath = Paths.get(templateName);
            byte[] data = Files.readAllBytes(xlsPath);
            log.debug("Filling properties in a hashtable.......");

            log.debug("Prepare excel workbooks.......");
            prepareExcelWorkbooksCR();

            createTemplateMappingsCR();
            writeReportForCR(rcaList);
            writeReportToBytes();

        } catch (BiffException e) {
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        log.debug("end generateAnalysisReport()");

        return finishedReport;


    }


}


