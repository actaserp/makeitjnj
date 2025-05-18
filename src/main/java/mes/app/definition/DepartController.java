package mes.app.definition;

import mes.app.definition.service.DepartService;
import mes.domain.entity.Depart;
import mes.domain.entity.User;
import mes.domain.model.AjaxResult;
import mes.domain.repository.DepartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/definition/depart")
public class DepartController {

	@Autowired
	DepartService departService;
	
	@Autowired
	DepartRepository departRepository;
	
	@GetMapping("/read")
	public AjaxResult getDepart(
			@RequestParam("keyword") String keyword) {
		
		List<Map<String, Object>> items = this.departService.getDepart(keyword);
		
		AjaxResult result = new AjaxResult();
		result.data = items;
		
		return result;
	}
	
	@GetMapping("/detail")
	public AjaxResult getDepartDetail(
			@RequestParam("id") int id,
			HttpServletRequest request) {
		
		Map<String, Object> item = this.departService.getDepartDetail(id);
		
		AjaxResult result = new AjaxResult();
		result.data = item;
		
		return result;
	}
	
	@PostMapping("/save")
	public AjaxResult saveDepart(
			@RequestParam(value="id", required=false) Integer id,
			@RequestParam(value="Code", required=false) String code,
			@RequestParam(value="Description", required=false) String description,
			@RequestParam(value="Name", required=false) String name,
			@RequestParam(value="Type", required=false) String type,
			HttpServletRequest request,
			Authentication auth
			) {
		
		User user = (User)auth.getPrincipal();
		
		Depart d = new Depart();
		
		if (id != null) {
			d = this.departRepository.getDepartById(id);
		}
		d.setCode(code);
		d.setName(name);
		d.setType(type);
		d.setDescription(description);
		d.set_audit(user);
		
		d = this.departRepository.save(d);
		
		AjaxResult result = new AjaxResult();
		result.data = d.getId();
		return result;
	}
	
	@PostMapping("/delete")
	public AjaxResult deleteDepart(@RequestParam("id") Integer id) {
		
		this.departRepository.deleteById(id);
		
		AjaxResult result = new AjaxResult();
		
		return result;
	}
	
}
