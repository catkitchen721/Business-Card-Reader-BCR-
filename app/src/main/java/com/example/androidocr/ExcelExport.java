package com.example.androidocr;

import java.io.File;

import jxl.CellType;
import jxl.Workbook;
import jxl.format.Alignment;
import jxl.format.Border;
import jxl.format.BorderLineStyle;
import jxl.format.CellFormat;
import jxl.format.Colour;
import jxl.format.Font;
import jxl.format.Format;
import jxl.format.Orientation;
import jxl.format.Pattern;
import jxl.format.VerticalAlignment;
import jxl.write.Label;
import jxl.write.WritableCell;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

public class ExcelExport {

    private Workbook  wbook;
    private WritableWorkbook wrWbook;
    private WritableSheet writeSheet;
    private jxl.write.Label writeLabel;

    public ExcelExport(String filePath, String name, String email, String phone)
    {
        try
        {
            File file = new File(filePath);
            if(!file.exists())
            {
                initExcel(file, name, email, phone);
            }
            else
            {
                updateExcel(file, name, email, phone);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void initExcel(File file, String name, String email, String phone)
    {
        try
        {
            this.wrWbook = Workbook.createWorkbook(file);
            this.writeSheet = wrWbook.createSheet("Information", 0);
            WritableCellFormat cfTitle = new WritableCellFormat(new WritableFont(WritableFont.ARIAL));
            cfTitle.setAlignment(Alignment.CENTRE);
            cfTitle.setBackground(Colour.GRAY_50);

            WritableCellFormat cf = new WritableCellFormat(new WritableFont(WritableFont.ARIAL));
            cf.setAlignment(Alignment.CENTRE);

            // 初始列
            this.writeLabel = new jxl.write.Label(0, 0, "Name");
            this.writeLabel.setCellFormat(cfTitle);
            writeSheet.addCell(writeLabel);
            this.writeLabel = new jxl.write.Label(1, 0, "E-mail");
            this.writeLabel.setCellFormat(cfTitle);
            writeSheet.addCell(writeLabel);
            this.writeLabel = new jxl.write.Label(2, 0, "Phone Number");
            this.writeLabel.setCellFormat(cfTitle);
            writeSheet.addCell(writeLabel);

            // 新增三個儲存格
            this.writeLabel = new jxl.write.Label(0, 1, name);
            this.writeLabel.setCellFormat(cf);
            writeSheet.addCell(writeLabel);
            this.writeLabel = new jxl.write.Label(1, 1, email);
            this.writeLabel.setCellFormat(cf);
            writeSheet.addCell(writeLabel);
            this.writeLabel = new jxl.write.Label(2, 1, phone);
            this.writeLabel.setCellFormat(cf);
            writeSheet.addCell(writeLabel);

            wrWbook.write(); // 把之前的操作都寫入到物件內
            wrWbook.close(); // 操作完成時，關閉物件，釋放佔用的記憶體空間
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void updateExcel(File file, String name, String email, String phone)
    {
        int rows = 0;
        try
        {
            this.wbook = Workbook.getWorkbook(file);
            this.wrWbook = Workbook.createWorkbook(file, wbook);
            this.writeSheet = wrWbook.getSheet(0);
            WritableCellFormat cf = new WritableCellFormat(new WritableFont(WritableFont.ARIAL));
            cf.setAlignment(Alignment.CENTRE);

            // 新增三個儲存格
            rows = writeSheet.getRows();
            this.writeLabel = new jxl.write.Label(0, rows, name);
            this.writeLabel.setCellFormat(cf);
            writeSheet.addCell(writeLabel);
            this.writeLabel = new jxl.write.Label(1, rows, email);
            this.writeLabel.setCellFormat(cf);
            writeSheet.addCell(writeLabel);
            this.writeLabel = new jxl.write.Label(2, rows, phone);
            this.writeLabel.setCellFormat(cf);
            writeSheet.addCell(writeLabel);

            wrWbook.write(); // 把之前的操作都寫入到物件內
            wrWbook.close(); // 操作完成時，關閉物件，釋放佔用的記憶體空間
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
