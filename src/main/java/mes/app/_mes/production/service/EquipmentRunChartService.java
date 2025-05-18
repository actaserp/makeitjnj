package mes.app.production.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import mes.domain.services.CommonUtil;
import mes.domain.services.SqlRunner;

@Service
public class EquipmentRunChartService {
	
	@Autowired
	SqlRunner sqlRunner;

	// // 차트 searchMainData
	public List<Map<String, Object>> getEquipmentRunChart(String date_from, String date_to, Integer id, String runType) {

		MapSqlParameterSource dicParam = new MapSqlParameterSource();
		
        dicParam.addValue("id", id);
        dicParam.addValue("runType", runType);
        
        String sql = """
        		select er.id
                , FORMAT(er."StartDate", 'yyyy-mm-dd') as start_date
                , FORMAT(er."EndDate", 'yyyy-mm-dd') as end_date
	            , e."Name"
	            , e."Code"
	            , er."StartDate"
	            , FORMAT(er."StartDate",'HH24:MI') as "StartTime"
	            , er."EndDate"
	            , FORMAT(er."EndDate",'HH24:MI') as "EndTime"
	            , EXTRACT(day from (er."EndDate" - er."StartDate")) * 60 * 24
	                + EXTRACT(hour from (er."EndDate" - er."StartDate")) * 60 
	                + EXTRACT(min from ("EndDate" - "StartDate")) as "GapTime"
                , er."WorkOrderNumber" 
	            , er."Equipment_id" 
	            , er."RunState" 
                , sc."StopCauseName" 
                , er."Description" 
                , er."StopCause_id"
                from equ_grp eg
                inner join equ e on eg.id = e."EquipmentGroup_id"
                left join equ_run er on e.id = er."Equipment_id"
                left join stop_cause sc on sc.id = er."StopCause_id"
                where 1=1
	            --and er."RunState" = :runType
        		""";
        
        if (id != null) {
        	sql += " and er.id = :id ";
		}else {
	        dicParam.addValue("date_from", CommonUtil.tryTimestamp(date_from));   
	        dicParam.addValue("date_to", Timestamp.valueOf(date_to + " 23:59:59"));
			
			sql += """
	        		and er."StartDate" <= :date_to
					and er."EndDate" >= :date_from
        		""";
		}
		
		sql += " order by e.\"Name\", er.\"StartDate\", er.\"EndDate\"";
  			    
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
        
        return items;
	}
}
