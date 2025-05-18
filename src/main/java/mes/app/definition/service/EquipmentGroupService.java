package mes.app.definition.service;

import io.micrometer.core.instrument.util.StringUtils;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class EquipmentGroupService {
	
	@Autowired
	SqlRunner sqlRunner;
	
	public List<Map<String, Object>> getEquipGroupList( String keyword) {
		MapSqlParameterSource dicParam = new MapSqlParameterSource();        
        dicParam.addValue("keyword", keyword);
        
        String sql = """
			select eg.id
            , eg."Name" as equipment_group_name
            , eg."Code" as equipment_group_code
            , fn_code_name('equipment_type', eg."EquipmentType"::text) as equipment_type
            from equ_grp eg 
            where 1=1
            """;
        if (StringUtils.isEmpty(keyword)==false) sql +="and upper(eg.\"Name\") like concat('%%',upper(:keyword),'%%')";
        
        sql +="order by id desc";
        
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
        return items;
	}
	
	public Map<String, Object> getEquipGroupDetail(Integer id) {
		MapSqlParameterSource dicParam = new MapSqlParameterSource();        
        dicParam.addValue("id", id);
        
        String sql = """
			select eg.id
		    , eg."Name" as equipment_group_name
		    , eg."Code" as equipment_group_code
		    , eg."EquipmentType" as equipment_type
		    from equ_grp eg 
		    where 1=1
		    and eg.id = :id
            """;

        Map<String, Object> items = this.sqlRunner.getRow(sql, dicParam);
        return items;
	}

}
