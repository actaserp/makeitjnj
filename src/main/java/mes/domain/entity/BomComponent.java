package mes.domain.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name="bom_comp")
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper=false)
public class BomComponent extends AbstractAuditModel {

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	int id;
	
	@Column(name = "\"BOM_id\"")
	int bomId;
	
	@Column(name = "\"Material_id\"")
	int materialId;


	@Column(name = "\"Amount\"")  //수량
	float amount;
	
	
	@Column(name = "\"Description\"")
	String description;	
	
	@Column(name = "\"_order\"")
	Integer _order;

	@Column(name = "spjangcd")
	String spjangcd;
	
}
