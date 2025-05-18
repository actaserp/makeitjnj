package mes.app.definition.service;

import io.micrometer.core.instrument.util.StringUtils;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DocFormSevice {
	
	@Autowired
	SqlRunner sqlRunner;	

	// 문서종류 리스트 조회
	public List<Map<String, Object>> getDocFormList(String formType, String keyword){
		MapSqlParameterSource dicParam = new MapSqlParameterSource(); 
		dicParam.addValue("formType", formType);
		dicParam.addValue("keyword", keyword);

        String sql = """
		   select df.id 
			    , df."FormGroup" as form_group
			    , df."FormName" as form_name
			    , df."Description" as description
	         from doc_form df 
	        where df."FormType" = :formType 
            """;
        if (StringUtils.isEmpty(keyword)==false) 
        	sql += "and upper(df.\"FormName\") like concat('%%',upper( :keyword ),'%%') ";

        sql += "order by df.\"FormName\" ";
        		
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
        
        return items;
	}
	
	// 문서종류 상세 조회
	public Map<String, Object> getDocFormDetail(Integer id){
		
		MapSqlParameterSource dicParam = new MapSqlParameterSource();        
        dicParam.addValue("id", id);
        
        String sql = """
		    select df.id 
		         , df."FormGroup" as form_group
		         , df."FormName" as form_name
		         , df."Description" as description
		      from doc_form df 
		     where 1=1
		       and df.id = :id
		    """;
        
        Map<String, Object> item = this.sqlRunner.getRow(sql, dicParam);
        
        return item;
	}

}
