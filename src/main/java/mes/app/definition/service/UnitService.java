package mes.app.definition.service;

import io.micrometer.core.instrument.util.StringUtils;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UnitService {
	
	@Autowired
	SqlRunner sqlRunner;
	

	// 단위 목록 조회
	public List<Map<String, Object>> getUnitList(String unitName){
		
		MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("unit_name", unitName);
        
        String sql = """
			select id, "Name" as name ,"Description" as description
		    , "PieceYN" as piece_yn
		    from unit  
		    where 1 = 1
		    """;
        if (StringUtils.isEmpty(unitName)==false) sql += "and upper(\"Name\") like concat('%%',upper( :unit_name ),'%%') ";
        sql += "order by id, \"Name\" ";
        
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
        
        return items;
	}
	
	// 단위 상세정보 조회
	public Map<String, Object> getUnitDetail(int unitId){
		
		MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("unit_id", unitId);
        
        String sql = """
			select id
		    , "Name" as name 
		    ,"Description" as description 
		    , "PieceYN" as piece_yn
		    from unit where id = :unit_id
		    """;
        
        Map<String, Object> item = this.sqlRunner.getRow(sql, dicParam);
        
        return item;
	}	
}
