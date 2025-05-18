package mes.app.definition;

import mes.app.definition.service.BomService;
import mes.domain.entity.Bom;
import mes.domain.entity.BomComponent;
import mes.domain.entity.User;
import mes.domain.model.AjaxResult;
import mes.domain.services.SqlRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;

@RestController
@RequestMapping("/api/definition/bom")
public class BomController {
	
	@Autowired
	SqlRunner sqlRunner;	
	
	@Autowired
	BomService bomService;	
	
	
	@RequestMapping("/read")
	public AjaxResult getMaterialList(
			@RequestParam(value="mat_type", required=false) String mat_type,
			@RequestParam(value="mat_group", required=false) Integer mat_group,
			@RequestParam(value="bom_type", required=false) String bom_type,
			@RequestParam(value="mat_name", required=false) String mat_name,
			@RequestParam(value="not_past_flag", required=false) String not_past_flag
			) {
		
		AjaxResult result = new AjaxResult();  
        result.data = this.bomService.getBomMaterialList(mat_type,mat_group,bom_type, mat_name, not_past_flag);            
		return result;
	}	
		

	
	@PostMapping("/save")
	public AjaxResult saveBom(
			@RequestParam(value="id", required = false) Integer id,
			@RequestParam(value="Name") String name,
			@RequestParam(value="Material_id") int materialId,
			@RequestParam(value="StartDate") String startDate,
			@RequestParam(value="EndDate") String endDate,
			@RequestParam(value="BOMType") String bomType,
			@RequestParam(value="Version") String version,
			@RequestParam(value="OutputAmount") float outputAmount,			
			Authentication auth	
			) {				
		
		User user = (User)auth.getPrincipal();
		
		AjaxResult result = new AjaxResult();
		
		startDate = startDate + " 00:00:00";
		endDate = endDate + " 23:59:59";
		
		Timestamp startTs = Timestamp.valueOf(startDate);
		Timestamp endTs = Timestamp.valueOf(endDate);
		
		boolean isSameVersion = this.bomService.checkSameVersion(id, materialId, bomType, version);
		
		if (isSameVersion==true) {
			result.success = false;
			result.message="중복된 BOM버전이 존재합니다.";
			return result;
		}
		
		boolean isDuplicated = this.bomService.checkDuplicatePeriod(id, materialId, bomType, startDate, endDate);
		if (isDuplicated) {
			result.success = false;
			result.message="기간이 겹치는 동일 제품의 \\n BOM이 존재합니다.";
			return result;			
		}
		
		Bom bom = null;
		if (id!=null) {
			bom = this.bomService.getBom(id);
		}else {
			bom = new Bom();
			if (StringUtils.hasText(version)==false) {
				version = "1.0";
			}
		}		
		
		bom.setName(name);
		bom.setMaterialId(materialId);
		bom.setOutputAmount(outputAmount);
		bom.setBomType(bomType);
		bom.setVersion(version);
		bom.setStartDate(startTs);
		bom.setEndDate(endTs);
		bom.set_audit(user);
				
		this.bomService.saveBom(bom);		
		result.data = bom.getId();
		
		return result;
		
	}	
	
	@RequestMapping("/detail")
	public AjaxResult getBomDetail(
			@RequestParam(value="id") int id
			) {
		AjaxResult result = new AjaxResult();		
        result.data = this.bomService.getBomDetail(id);		
		return result;		
	}
	
	@RequestMapping("/bom_delete")
	public AjaxResult deleteBom(
			@RequestParam(value="id") int id
			) {
		
		MapSqlParameterSource paramMap = new MapSqlParameterSource();		
		AjaxResult result = new AjaxResult();		
		String sql = "delete from bom where id=:id ";
		paramMap.addValue("id", id);		
		int iRowEffected = this.sqlRunner.execute(sql, paramMap);
		result.data = iRowEffected;
		return result;
	}
	
	@RequestMapping("/material_save")
	public AjaxResult bomComponentSave(
			@RequestParam(value="id" , required = false) Integer id,
			@RequestParam(value="BOM_id") int bom_id,
			@RequestParam(value="Material_id") int materialId,
			@RequestParam(value="Amount") float amt,
			@RequestParam(value="_order",required = false) Integer _order,
			@RequestParam(value="Description",required = false) String description,
			Authentication auth			
			) {
		
		User user = (User)auth.getPrincipal();
		AjaxResult result = new AjaxResult();
		
		BomComponent bomComponent = null;
		
		if (id !=null) {
			// 기존 데이터를 가져온다
			bomComponent = this.bomService.getBomComponent(id);			
		}else {
			//동일한 데이터가 있는지 검사해서 중복이 있으면 리턴
			//신규데이터를 등록한다
			boolean exists = this.bomService.checkDuplicateBomComponent(bom_id, materialId);
			if(exists) {
				result.success=false;
				result.message = "이미 존재하는 품목입니다.";
				return result;
			}			
			bomComponent = new BomComponent();
		}
		
		bomComponent.setBomId(bom_id);
		bomComponent.setMaterialId(materialId);
		bomComponent.setAmount(amt);
		bomComponent.set_order(_order);
		bomComponent.setDescription(description);		
		bomComponent.set_audit(user);
		
		bomComponent = this.bomService.saveBomComponent(bomComponent);
		result.data = bomComponent.getId();
		return result;		
	}
	
	@RequestMapping("/material_detail")
    public AjaxResult bomComponentDetail(
    		@RequestParam(value="id") int id    		
    		) {
    	AjaxResult result = new AjaxResult();    	
    	result.data = this.bomService.getBomComponentDetail(id);    	
    	return result;
    }	
	

	@PostMapping("/material_delete")
	public AjaxResult deleteBomComponent(
			@RequestParam(value="id") int id
			) {
		AjaxResult result = new AjaxResult();		
		result.data = this.bomService.deleteBomComponent(id);		
		return result;		
	}
		
	
	@RequestMapping("/bom_comp_list")
	public AjaxResult getBomCompList(
			@RequestParam(value="id") Integer id
			) {
		AjaxResult result = new AjaxResult();
		String sql = """
	            select bc.id
	            , dbo.fn_code_name('mat_type', mg."MaterialType") as mat_type
	            , mg."Name" as group_name
	            , m."Name" as mat_name
	            , m."Code" as mat_code
	            , bc."Amount"
	            , bc."Material_id" as mat_id
	            , m."Unit_id"
	            , u."Name" as unit
	            , bc."Description"
	            , bc."_order" 
	            from bom_comp bc
	            left join material m on bc."Material_id"=m.id
	            left join unit u on u.id = m."Unit_id" 
	            left join mat_grp mg on m."MaterialGroup_id" =mg.id
	            where bc."BOM_id" = :bom_id
	            order by bc."_order"
	    """;		
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("bom_id", id);
		result.data = this.sqlRunner.getRows(sql, paramMap);
		
		return result;		
	}
	
	@RequestMapping("/material_tree_list")
	public AjaxResult getComponentTreeList(
			@RequestParam(value="id") Integer id
			) {		
		AjaxResult result = new AjaxResult();		
		result.data = this.bomService.getBomComponentTreeList(id);		
		return result;		
	}	
	
	
	@PostMapping("/bom_replicate")
	public AjaxResult bomReplicate(
			@RequestParam(value="id") int bom_id,
			Authentication auth
			) {		
		
		User user = (User)auth.getPrincipal();				
		return this.bomService.bomReplicate(bom_id, user);
	}	
	
	@PostMapping("/bom_revision")
	public AjaxResult bomRevision(
			@RequestParam(value="id") int bom_id,
			Authentication auth
			) {		
		User user = (User)auth.getPrincipal();
		return this.bomService.bomRevision(bom_id, user);
	}

	// 텍스트 + 순서저장은 저장 코드가 없음

}