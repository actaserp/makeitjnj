package mes.app.system.service;

import java.util.List;
import java.util.Map;

import mes.domain.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import mes.domain.services.SqlRunner;
@Service 
public class UserGroupService {
	
	@Autowired
	SqlRunner sqlRunner;

	public List<Map<String,Object>> getUserGroupList(Boolean super_user) {
		String sql = """
				SELECT
				    id,
				    "Code" AS code,
				    "Name" AS name,
				    "Description" AS description,
				    "Disabled" AS disabled,
				    CASE
				        WHEN EXISTS (SELECT 1 FROM user_profile up WHERE up."UserGroup_id" = ug.id) THEN 'Y'
				        ELSE 'N'
				    END AS flag,
				    "gmenu" AS gmenu,
				    mi."MenuName" AS gmenuname,
				    FORMAT(ug."_created", 'yyyy-MM-dd HH:mm:ss') AS created  -- 또는 CONVERT(varchar, ug."_created", 120)
				FROM user_group ug
				LEFT JOIN menu_item mi ON mi."MenuCode" = ug.gmenu
				WHERE 1 = 1
			""";


		if (super_user == false) {
			sql += "and \"Code\" <> 'dev' ";
		}


		sql += " order by \"Name\" ";
		List<Map<String,Object>> items = this.sqlRunner.getRows(sql, null);
		return items;

	}
	
	public Map<String, Object> getUserGroup(int id) {
		String sql = """
				SELECT id,
				       [Code] AS code,
				       [Name] AS name,
				       [Description] AS description,
				       [Disabled] AS disabled,
				       CONVERT(VARCHAR, [_created], 120) AS created
				FROM user_group;
				WHERE id = :group_id;
				""";
		MapSqlParameterSource dicParam = new MapSqlParameterSource();
		dicParam.addValue("group_id", id);
		
		Map<String , Object> item = this.sqlRunner.getRow(sql, dicParam);
		return item;
	}

	public Map<String, Object> getDefaultMenu(User user) {

		String sql = """
			select u."gmenu", m."MenuName"
			from user_group u
			join menu_item m on u.gmenu = m."MenuCode"
            where id = :group_id
			""";
		MapSqlParameterSource dicParam = new MapSqlParameterSource();
		dicParam.addValue("group_id", user.getUserProfile().getUserGroup().getId());

		Map<String, Object> item = this.sqlRunner.getRow(sql, dicParam);
		return item;
	}
	
	
}