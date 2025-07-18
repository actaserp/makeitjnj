package mes.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import mes.domain.entity.Bom;

public interface BomRepository extends JpaRepository<Bom, Integer>{
	public Bom getBomById(int id);

  Bom findByMaterialIdAndBomTypeAndVersion(int productId, String manufacturing, String s);
}
