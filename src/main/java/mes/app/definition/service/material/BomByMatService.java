package mes.app.definition.service.material;

import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class BomByMatService {
	
	@Autowired
	SqlRunner sqlRunner;

	public List<Map<String, Object>> getBomListByMat(String matPk){
		MapSqlParameterSource dicParam = new MapSqlParameterSource();        
        dicParam.addValue("mat_pk", matPk);
        
        String sql = """
			select bom.b_level as _level
                , m."Name" as mat_name
                , bom.bom_ratio
                , concat(bom.quantity,'/',bom.produced_qty) as bom_qty
                , dbo.fn_code_name('mat_type',mg."MaterialType") as mat_type
                , mat_pk, parent_mat_pk
                , u."Name" as unit
                , m."Code" as mat_code
                , bom.mat_pk as my_key
                , bom.parent_mat_pk as parent_key
	            FROM tbl_bom_detail(:mat_pk, FORMAT(GETDATE(), 'yyyy-MM-dd')) AS bom
                inner join material m on m.id = bom.mat_pk
                left join mat_grp mg on mg.id = m."MaterialGroup_id"
                left join unit u on u.id = m."Unit_id"
	            order by tot_order
        """;
        	
        
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
        return items;
	}
	
	public List<Map<String, Object>> getBomReverseListByMat(int matPk){
		MapSqlParameterSource dicParam = new MapSqlParameterSource();        
        dicParam.addValue("mat_pk", matPk);
        
        String sql = """
						SELECT\s
						bom.b_level AS _level,
						m.Name AS mat_name,
						bom.bom_ratio,
						CAST(bom.quantity AS VARCHAR) + '->' + CAST(bom.produced_qty AS VARCHAR) AS bom_qty,
						sc.Value as mat_type,
						u.Name AS unit,
						m.Code AS mat_code,
						bom.prod_pk AS my_key,
						bom.parent_prod_pk AS parent_key
						FROM dbo.tbl_bom_reverse(:mat_pk, CONVERT(VARCHAR, GETDATE(), 23)) AS bom
						INNER JOIN material m ON m.id = bom.prod_pk
						LEFT JOIN mat_grp mg ON mg.id = m.MaterialGroup_id
						LEFT JOIN unit u ON u.id = m.Unit_id
						LEFT JOIN sys_code sc on sc.Code = mg.MaterialType and sc.CodeType = 'mat_type'
						ORDER BY bom.tot_order;
        """;
        	
        //프로시저가 없음
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
        return items;
	}
}
