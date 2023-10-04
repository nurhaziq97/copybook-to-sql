package com.copybookExperiments.cbJava;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class cbReader {
    public static void main(String[] args) {
        System.out.println("This application is used to generate SQL query from COBOL copybook");
        Scanner scannerString = new Scanner(System.in);

        // System.out.print("Enter copybook file path:");
        String filePath = "/home/haziq/experimental/copybookExperiments/copybookExample.txt";
        // filePath = scannerString.nextLine();

        File file;
        file = new File(filePath);
        List<String> lines = new ArrayList<>();

        try {
            FileInputStream fileInputStream;
            fileInputStream = new FileInputStream(file);
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line;
            String tempLine = new String();
            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();
                // System.out.println(line);
                if (line.indexOf(".") < 0) {
                    // hold and concat string
                    tempLine = tempLine.concat(line).concat(" ");
                } else {
                    line = tempLine.concat(line);
                    tempLine = new String();
                    // System.out.println(line);
                    lines.add(line);
                }
            }

            parseLineToGetCBColumnObject(lines);

            bufferedReader.close();

        } catch (IOException e) {
            System.out.println("File not found");
            e.printStackTrace();
        }

        scannerString.close();
    }

    private static List<CBColumn> parseLineToGetCBColumnObject(List<String> lines) {
        List<CBColumn> cbColumns = new ArrayList<>();
        for (String line : lines) {
            CBColumn cbColumn = new CBColumn();
            String[] words = line.split("\\s+");
            for (int i = 0; i < words.length; i++) {
                String word = words[i];
                if (i == 0) {
                    // get level number
                    try {
                        Integer.parseInt(word);
                        cbColumn.setLevelNumber(word);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
                if(i == 1) {
                    // get dataName
                    String underscoredWord = word.replaceAll("-", "_");
                    try {
                        if (Integer.parseInt(cbColumn.getLevelNumber()) >= 1
                                && Integer.parseInt(cbColumn.getLevelNumber()) <= 49) {
                            cbColumn.setDataName(underscoredWord.replace(".", ""));
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
                if (word.equalsIgnoreCase("PIC")) {
                    word = words[i+1];
                    // set data type
                    // COBOL copybook has only 6 type data type but can be combine
                    // 9 - Numeric => DECIMAL
                    // A - Alphabetic => VARCHAR
                    // X - Alphaneumeric e.g: X(2) => VARCHAR
                    // V - Implicit decimal e.g: 9(2)V9(2) 23.33 => DECIMAL (Important is after the
                    // V)
                    // S - sign(+/-) DECIMAL
                    // P - assumed decimal e.g: PP99@PPPP@P999 .2319 => DECIMAL
                    if (word.indexOf("A") >= 0 || word.indexOf("X") >= 0) {
                        int varcharQty = -1;
                        int aFirstPos = word.indexOf("A");
                        int xFirstPos = word.indexOf("X");
                        int firstPos = aFirstPos > 0 ? aFirstPos : xFirstPos;
                        String charUsed = aFirstPos > 0 ? "A" : "X";
                        // verify if after A or X has bracket or multiple X or A
                        // means that if A(9) translate to VARCHAR(9), if AA translate to VARCHAR(2)
                        if (firstPos == word.lastIndexOf(charUsed)) {
                            varcharQty = firstPos + 1;
                        }
                        if (word.lastIndexOf(charUsed) != firstPos) {
                            // case XXX
                            varcharQty = (word.lastIndexOf(charUsed) + 1) - firstPos;
                        }
                        if (word.charAt(firstPos + 1) == '(') {
                            // case X(2) or A(2)
                            String numberInBracket = new String();
                            numberInBracket = word.substring(firstPos + 2, word.indexOf(")"));
                            // System.out.println("NumberInBracket: " + numberInBracket);
                            try {
                                varcharQty = Integer.parseInt(numberInBracket);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                        String sqlQryStr = varcharQty < 0 ? "VARCHAR" : "VARCHAR(" + varcharQty + ")";
                        cbColumn.setDataTypeSQL(sqlQryStr);
                    }
                    if (word.contains("9") && !word.contains("P")) {
                        int sqlSignQty = -1;
                        int sqlImplicitDecQty = -1;
                        int sql9Qty = -1;
                        if (word.contains("S9")) {
                            // word is decimal with the sign
                            // S9(2)V9(2)
                            int firstPosS9 = word.indexOf("S9");

                            if (word.charAt(firstPosS9 + 1) == '(') {
                                String numberInBracket = word.substring(firstPosS9 + 2, word.indexOf(")"));
                                try {
                                    sqlSignQty = Integer.parseInt(numberInBracket);
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }
                            } else if (word.charAt(firstPosS9 + 1) == '9') {
                                // S9999
                                int countQty = 0;
                                for (int indexOf9 = 1; word.charAt(firstPosS9 + indexOf9) == '9'; indexOf9++) {
                                    countQty++;
                                }
                                sqlSignQty = countQty;
                            }
                        }
                        if (word.contains("V9")) {
                            // word is decimal with the sign
                            // S9(2)V9(2)
                            int firstPosV9 = word.indexOf("V9");
                            if (word.charAt(firstPosV9 + 1) == '(') {
                                String numberInBracket = word.substring(firstPosV9 + 2, word.indexOf(")"));
                                try {
                                    sqlImplicitDecQty = Integer.parseInt(numberInBracket);
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }
                            } else if (word.charAt(firstPosV9 + 1) == '9') {
                                // S9(2)V999
                                int countQty = 0;
                                for (int indexOf9 = 1; word.charAt(firstPosV9 + indexOf9) == '9'; indexOf9++) {
                                    countQty++;
                                }
                                sqlImplicitDecQty = countQty;
                            }else {
                                sqlImplicitDecQty = 1;
                            }
                        }
                        String tempWord  = word.replace("V9","");
                        if (!tempWord.contains("S") && !tempWord.contains("V")) {
                            int firstPos9 = word.indexOf('9');
                            if (firstPos9 >= 0) {
                                sql9Qty = 1;
                                if (word.charAt(firstPos9+1) == '(') {
                                    String numberInBracket = word.substring(firstPos9 + 2, word.indexOf(")"));
                                    try {
                                        sql9Qty = Integer.parseInt(numberInBracket);
                                    } catch (NumberFormatException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }else if(word.charAt(firstPos9+1) == '9'){
                                //9999
                                int countQty = 0;
                                for (int indexOf9 = 1; word.charAt(firstPos9 + indexOf9) == '9'; indexOf9++) {
                                    countQty++;
                                }
                                sql9Qty = countQty + 1;
                            }
                        }

                        String sqlQryStr = "DECIMAL(";
                        // System.out.println("SQL Sign Qty : "+ sqlSignQty + "\n" + "SQL 9 Quantity: " + sql9Qty + "\nSQL V Qty: " + sqlImplicitDecQty);
                        if((sqlSignQty > 0 || sql9Qty > 0) && sqlImplicitDecQty > 0){
                            sqlQryStr += sqlSignQty > 0 ? sqlSignQty : sql9Qty;
                            sqlQryStr += ",";
                            sqlQryStr += sqlImplicitDecQty;
                            sqlQryStr += ")";

                        }else if((sqlSignQty < 0 || sql9Qty < 0) && sqlImplicitDecQty < 0){
                            sqlQryStr = "DECIMAL";
                        }
                        cbColumn.setDataTypeSQL(sqlQryStr);
                    }

                    if (word.contains("P")) {
                        int sqlAssumedDecQty = -1;
                        // word is decimal with the sign
                        // P9(2)
                        int firstPosP = word.indexOf("P");

                    }
                    word = word.replace(".","");
                    cbColumn.setDataType(word);
                }
                if(word.equals("VALUE")){
                    word = words[i+1];
                    cbColumn.setDefaultValue(word);
                }

                // considers list is sorted out
                cbColumns.add(cbColumn);

            }
            System.out.println(cbColumn);
        }

        return null;
    }

    public static void generateNestedList(List<CBColumn> cbColumns, int current) {
        if (current > 0) {
            int beforeCurrent = current - 1;
            if (Integer.parseInt(cbColumns.get(current).getLevelNumber()) > Integer
                    .parseInt(cbColumns.get(beforeCurrent).getLevelNumber())
                    && (Integer.parseInt(cbColumns.get(current).getLevelNumber()) >= 1
                            && Integer.parseInt(cbColumns.get(current).getLevelNumber()) <= 49)) {
                List<CBColumn> cbColumnsElements = cbColumns.get(beforeCurrent).getCBColumns();
                cbColumnsElements.add(cbColumns.get(current));
                cbColumns.remove(current);
            }
        }
    }

    public static class CBColumn {
        private String levelNumber;
        private String dataName;
        private String dataType;
        private String dataTypeSQL;
        private String defaultValue;
        private List<CBColumn> cbColumns;

        public void setLevelNumber(String levelNumber) {
            this.levelNumber = levelNumber;
        }

        public String getLevelNumber() {
            return this.levelNumber;
        }

        public void setDataName(String dataName) {
            this.dataName = dataName;
        }

        public String getDataName() {
            return this.dataName;
        }

        public void setDataType(String dataType) {
            this.dataType = dataType;
        }

        public String getDataType() {
            return this.dataType;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String getDefaultValue() {
            return this.defaultValue;
        }

        public void setCBColumns(List<CBColumn> cbColumns) {
            this.cbColumns = cbColumns;
        }

        public List<CBColumn> getCBColumns() {
            return this.cbColumns;
        }

        public void setDataTypeSQL(String dataTypeSQL) {
            this.dataTypeSQL = dataTypeSQL;
        }

        public String getDataTypeSQL() {
            return this.dataTypeSQL;
        }

        public CBColumn(String levelNumber, String dataName, String dataType, String defaultValue) {
            this.levelNumber = levelNumber;
            this.dataName = dataName;
            this.dataType = dataType;
            this.defaultValue = defaultValue;
        }

        @Override
        public String toString() {
            return "CBColumn{" +
                    "levelNumber='" + levelNumber + '\'' +
                    ", dataName='" + dataName + '\'' +
                    ", dataType='" + dataType + '\'' +
                    ", defaultValue='" + defaultValue + '\'' +
                    ", dataTypeSQL='" + dataTypeSQL + '\'' +
                    '}';
        }

        public CBColumn() {
        }
    }
}