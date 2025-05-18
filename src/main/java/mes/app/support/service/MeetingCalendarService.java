package mes.app.support.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import mes.domain.services.SqlRunner;

@Service
public class MeetingCalendarService {

	@Autowired
	SqlRunner sqlRunner;

	public List<Map<String, Object>> getMeetingCalendar(String keyword) {
		
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		String dateFrom = keyword + "-01";
		paramMap.addValue("dateFrom", dateFrom);
		
		String sql = """
				select id::text
                , "Title" as title
                , FORMAT("DataDate",'yyyy-mm-dd') as "DataDate"
                , FORMAT("DataDate",'yyyy-mm-dd')||' '||FORMAT("StartTime",'hh24:mi') as start
                , FORMAT("DataDate",'yyyy-mm-dd')||' '||FORMAT("EndTime",'hh24:mi') as end
                , "Color" as color
                , 'calendar' as data_div
                , "Description" as description
                from calendar
                where "DataDate" between cast(:dateFrom as date) and cast(:dateFrom as date) + interval '1 month - 1 day'
                union all
                SELECT 'mo'|| mo.id::text
                --, true as allDay
		        --, mo."OrderNumber"
		        --, mo."AvailableStock"
		        , concat(m."Name",' ',mo."OrderQty", u."Name") as title
                , FORMAT(mo."InputPlanDate",'yyyy-mm-dd') as "DataDate"
                , FORMAT(mo."InputPlanDate",'yyyy-mm-dd')||' 00:00' as start
                , FORMAT(mo."InputPlanDate",'yyyy-mm-dd')||' 00:00' as end
	            , '#FF0000' as color
                , 'mat_order' as data_div
                , mo."Description" as description
	            from mat_order mo
	            inner join material m ON m.id = mo."Material_id"
                LEFT JOIN unit u ON m."Unit_id" = u.id
                WHERE mo."InputPlanDate" between cast(:dateFrom as date) and cast(:dateFrom as date) + interval '1 month - 1 day'
                and mo."State" = 'approved' 
				""";
		
		List<Map<String,Object>> items = this.sqlRunner.getRows(sql, paramMap);
		
		return items;
	}
}
