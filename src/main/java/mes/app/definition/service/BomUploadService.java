package mes.app.definition.service;

import lombok.extern.slf4j.Slf4j;
import mes.domain.repository.BomRepository;
import mes.domain.services.CommonUtil;
import mes.domain.services.SqlRunner;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class BomUploadService {

	@Autowired
	SqlRunner sqlRunner;
	
	@Autowired
	BomRepository bomRepository;


	

	// 수주 업로드 내역 조회 
	public List<Map<String, Object>> getSujuUploadList(String date_kind, String start, String end, String spjangcd) {
		
		MapSqlParameterSource dicParam = new MapSqlParameterSource();
		// data_kind : 'sales', 'delivery'
		dicParam.addValue("date_kind", date_kind);
		dicParam.addValue("start", start);
		dicParam.addValue("end", end);
		dicParam.addValue("spjangcd", spjangcd);
		
		String sql = """
			select 
            sb.id
            ,case when sb._status = 'Excel' then '엑셀' else '수주' end as state
            ,sb."JumunNumber" as jumun_number
            ,sb."CompCode" as company_code
            ,sb."CompanyName" as company_name
            ,sb."ProductCode" as product_code
            ,sb."ProductName" as product_name
            ,u."Name" as unit
            ,sb."Quantity" as suju_qty
            ,sb."JumunDate" as jumun_date
            ,sb."DueDate" as due_date
            from suju_bulk sb
            inner join material m on m."Code" = sb."ProductCode"
            --inner join material m on m.id = sb.id
            inner join unit u on u.id = m."Unit_id"
            where 1 = 1
            and sb.spjangcd = :spjangcd
			""";
		
		if (date_kind.equals("sales")) {
			sql += """
				and sb."JumunDate" between :start and :end
                order by sb."JumunDate" desc
				""";
		} else {
			sql +="""
				and sb."DueDate" between :start and :end
                order by sb."DueDate" desc
				""";
		}
		
		List<Map<String, Object>> itmes = this.sqlRunner.getRows(sql, dicParam);
		
		return itmes;
	}

	// 엑셀파일 파싱
	public List<List<String>> excel_read(String filename) throws IOException {
		List<List<String>> all_rows = new ArrayList<>();
		FileInputStream file = new FileInputStream(filename);
		XSSFWorkbook wb = new XSSFWorkbook(file);
		XSSFSheet sheet = wb.getSheetAt(0);

		for (int i = 0; i <= sheet.getLastRowNum(); i++) {
			XSSFRow row = sheet.getRow(i);
			if (row == null) continue; // 완전 빈행 skip

			XSSFCell jumunNumCell = row.getCell(0);
			if (jumunNumCell == null || jumunNumCell.getCellType() == CellType.BLANK) {
				continue; // 빈행 skip, 전체 데이터는 계속
			}

			List<String> value_list = new ArrayList<>();
			for (int j = 0; j < row.getLastCellNum(); j++) {
				XSSFCell cell = row.getCell(j);
				if (cell != null) {
					if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
						value_list.add(new SimpleDateFormat("yyyy-MM-dd").format(cell.getDateCellValue()).strip());
					} else if (cell.getCellType() == CellType.NUMERIC) {
						value_list.add(CommonUtil.tryString(cell.getNumericCellValue()).strip());
					} else {
						value_list.add(cell.getStringCellValue().strip());
					}
				} else {
					value_list.add(""); // 셀이 null이면 빈값으로 넣기(셀누락 방지)
				}
			}
			all_rows.add(value_list);
		}

		wb.close();
		return all_rows;
	}

	public int SaveUnitPrice(Map<String, Object> data) {
		MapSqlParameterSource param = new MapSqlParameterSource();

		String pname = (String) data.get("PNAME");
		String psize = (String) data.get("PSIZE"); // null일 수 있음
		Object puamt = data.get("PUAMT");
		String inputDate = (String) data.get("INPUTDATE");
		String pcode = (String) data.get("PCODE"); // 무조건 있음
		String cltcd = (String) data.get("CLTCD"); // null일 수 있음

		param.addValue("pname", pname);
		param.addValue("psize", psize);
		param.addValue("puamt", puamt);
		param.addValue("inputdate", inputDate);
		param.addValue("pcode", pcode);

		if (cltcd != null && !cltcd.isBlank()) {
			param.addValue("cltcd", cltcd);
		}

		StringBuilder sql = new StringBuilder();
		sql.append("""
    MERGE INTO mat_uamt AS target
    USING (
        SELECT :pcode AS PCODE,
               :pname AS PNAME,
               :psize AS PSIZE,
               :puamt AS PUAMT,
               :inputdate AS INPUTDATE
""");
		if (cltcd != null && !cltcd.isBlank()) {
			sql.append(", :cltcd AS CLTCD\n");
		}
		sql.append("""
    ) AS source
    ON target.PCODE = source.PCODE AND target.PNAME = source.PNAME
    WHEN MATCHED AND (
        (target.PUAMT IS NULL AND source.PUAMT IS NOT NULL) OR
        (target.PUAMT IS NOT NULL AND source.PUAMT IS NULL) OR
        (target.PUAMT <> source.PUAMT)
    )
    THEN UPDATE SET
        PUAMT = source.PUAMT,
        INPUTDATE = source.INPUTDATE
""");
		if (cltcd != null && !cltcd.isBlank()) {
			sql.append(", CLTCD = source.CLTCD\n");
		}
		sql.append("""
    WHEN NOT MATCHED THEN
    INSERT (PCODE, PNAME, PSIZE, PUAMT, INPUTDATE
""");
		if (cltcd != null && !cltcd.isBlank()) {
			sql.append(", CLTCD");
		}
		sql.append("""
    )
    VALUES (source.PCODE, source.PNAME, source.PSIZE, source.PUAMT, source.INPUTDATE
""");
		if (cltcd != null && !cltcd.isBlank()) {
			sql.append(", source.CLTCD");
		}
		sql.append(");\n");


//		log.info("단가 저장 SQL: {}", sql);
//		log.info("SQL Parameters: {}", param.getValues());

		return sqlRunner.execute(sql.toString(), param);
	}


}
