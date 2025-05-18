package mes.app.definition;

import mes.app.definition.service.AreaService;
import mes.domain.entity.Area;
import mes.domain.entity.User;
import mes.domain.model.AjaxResult;
import mes.domain.repository.AreaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/api/definition/area")
public class AreaController {
	
	@Autowired 
	AreaRepository areaRepository;
	
	@Autowired
	private AreaService areaService;
	
	@GetMapping("/read")
	public AjaxResult getAreaList(
			@RequestParam("txtName") String txtName,
			HttpServletRequest request) {
		List<Map<String, Object>> items = this.areaService.getAreaList(txtName);
		
		AjaxResult result = new AjaxResult();
		result.data = items;
		
		return result;
	}
	
	@GetMapping("/detail")
	public AjaxResult getArea(
			@RequestParam("id") int id,
			HttpServletRequest request) {
		//Map<String, Object> item = this.areaService.getArea(id, txtName);
		Optional<Area> optArea = this.areaRepository.findById(id);
		
		Area area = null;
		if (optArea.isPresent()) {
			area = optArea.get();
		}
		AjaxResult result = new AjaxResult();
		
		
		result.data = area;
		
		return result;
		
	}
	
	@PostMapping("/save")
	public AjaxResult saveArea(
			@RequestParam(value="id" , required=false) Integer id,
			@RequestParam(value="factory_id", required=false) Integer factory_id,
			@RequestParam("name") String name,
			@RequestParam(value="parent_id" , required=false) Integer parent_id,
			@RequestParam("description") String description,
			HttpServletRequest request,
			Authentication auth) {
		
		AjaxResult result = new AjaxResult();
		
		User user = (User)auth.getPrincipal();
		
		Area area = null;
		
		if(id == null) {
			area = new Area();
		} else {
			area = this.areaRepository.getAreaById(id);
		}
		
		boolean nameChk = this.areaRepository.findByName(name).isEmpty();
		
		if(name.equals(area.getName()) == false && nameChk == false) {
			result.success = false;
			result.message = "이미 존재하는 이름입니다.";
			return result;	
		}
		
		area.setName(name);
		area.setDescription(description);
		area.setFactory_id(factory_id);
		area.setParent_id(parent_id);
		area.set_audit(user);
		
		area = this.areaRepository.save(area);
		
		result.data = area;
		return result;

	}
	
	
	@PostMapping("/delete")
	public AjaxResult deleteArea(@RequestParam("id") Integer id) {
		this.areaRepository.deleteById(id);
		AjaxResult result = new AjaxResult();
		
		return result ;
	}
	

}
	

