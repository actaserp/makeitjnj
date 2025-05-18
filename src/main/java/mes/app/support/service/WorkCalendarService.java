package mes.app.support.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import mes.domain.services.SqlRunner;

@Service
public class WorkCalendarService {

	@Autowired
	SqlRunner sqlRunner;
	
	public List<Map<String, Object>> getWorkCalendar(String dataMonth, String factoryPk) {

		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("dataMonth", dataMonth + "-01");
		paramMap.addValue("factoryPk", factoryPk);
		
		String sql = """
                select wc.id as _id
	            , wc."DataDate"
	            , FORMAT(wc."DataDate",'yyyy-mm-dd') ||' '|| coalesce(FORMAT(wc."StartTime",'hh24:mi'),'00:00') as start
	            , FORMAT(wc."DataDate",'yyyy-mm-dd') ||' '|| coalesce(FORMAT(wc."EndTime",'hh24:mi'),'00:00') as end
	            --, wc."DataPk"  as title
	            , wc."DataPk"  as factory_pk
                , f."Name" as factory_name
	            , wc."WorkHr" as worktime	
                , wc."HolidayYN" as holiday_yn
	            from work_calendar wc
                left join factory f on f.id = wc."DataPk" 
                WHERE wc."DataDate" between cast(:dataMonth as date) and cast(:dataMonth as date) + interval '1 month - 1 day'
                and wc."TableName" = 'factory'
				""";
		
        if(StringUtils.hasText(factoryPk)) {
        	sql += " and wc.\"DataPk\" = cast(:factoryPk as Integer) ";
        }

        sql += " order by wc.\"DataDate\", wc.id ";
        
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, paramMap);
        
		return items;
	}

}
