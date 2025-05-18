package mes.app.definition.service;

import io.micrometer.core.instrument.util.StringUtils;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PersonService {

	@Autowired
	SqlRunner sqlRunner;
	
	public List<Map<String, Object>> getPersonList(String workerName, String workcenterId) {
		
		MapSqlParameterSource dicParam = new MapSqlParameterSource();        
        dicParam.addValue("workerName", workerName);
        dicParam.addValue("workcenterId", workcenterId);
        
        String sql = """
        		SELECT p.id
		        , p."Name" as person_name 
		        , p."Code" as person_code
		        , wc."Name" as workcenter_name
		        , p."WorkHour" as work_hour
		        , p."Description" as description
	            , p."WorkCenter_id" as workcenter_id
	            , p."Factory_id" as factory_id
	            , p."ShiftCode" as shift_code
	            , sh."Name" as shift_name
	            , d."Name" as dept_name
	            , f."Name" as factory_name
	            , p."PersonGroup_id" as person_group_id
	            , s."Value" as jik_id
		        FROM person p
		        left join work_center wc on p."WorkCenter_id" = wc.id
		        left join Factory f on p."Factory_id" = f.id
	            left join shift sh on sh."Code" = p."ShiftCode"
	            left join depart d on d.id = p."Depart_id"
	            left join (
				        SELECT "Code", "Value"
				        FROM sys_code
				        WHERE "CodeType" = 'jik_type'
				) s on s."Code" = p.jik_id
	            where 1=1
        		""";
        if (StringUtils.isEmpty(workerName)==false) sql +=" and upper(p.\"Name\") like concat('%%',upper(:workerName),'%%') ";
        if (StringUtils.isEmpty(workcenterId)==false) sql +=" and p.\"WorkCenter_id\" = cast(:workcenterId as Integer) ";
        
        sql += " order by p.id ";
        
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
        
        return items;
	}

	public Map<String, Object> getPersonDetail(Integer id) {
		
		MapSqlParameterSource dicParam = new MapSqlParameterSource();        
        dicParam.addValue("id", id);
        
        String sql = """
             SELECT p.id
	        , p."Name"
	        , p."Code"
	        , wc."Name" as workcenter_bame
	        , p."WorkHour"
            , p."Description"
            , p."WorkCenter_id"
            , p."Factory_id"
            , p."ShiftCode"
            , sh."Name" as shift_name
            , p."Depart_id"
            , d."Name" as dept_name
	        FROM person p
	        LEFT JOIN work_center wc on p."WorkCenter_id" = wc.id
	        LEFT JOIN Factory f on p."Factory_id" = f.id
            left join shift sh on sh."Code" = p."ShiftCode"
            left join depart d on d.id = p."Depart_id"
            where 1=1
            and p.id = :id
		    """;
        
        Map<String, Object> item = this.sqlRunner.getRow(sql, dicParam);
        
        return item;
	}

}
