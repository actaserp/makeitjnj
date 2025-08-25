package mes.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import mes.domain.entity.Material;

import java.util.Optional;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Integer>{

	Material getMaterialById(Integer matPk);
	
	Integer countByIdAndStoreHouseIdIsNull(Integer id);
	Material findByCode(String string);

	@Query("SELECT m FROM Material m WHERE TRIM(m.name) = TRIM(:materialName)")
	Material findByNameTrimmed(@Param("materialName") String materialName);

	@Query(value = "SELECT MAX(CAST(\"Code\" AS INTEGER)) FROM material WHERE LENGTH(\"Code\") = 4 AND \"Code\" ~ '^[0-9]{4}$'", nativeQuery = true)
	String findMaxCodeBy4000Prefix();

	boolean existsByCode(String s);

	@Query(value = "SELECT TOP 1 Code FROM material WHERE Code LIKE '2000%' AND ISNUMERIC(Code) = 1 ORDER BY CAST(Code AS INT) DESC", nativeQuery = true)
	String findMaxCodeForMaterial();

	@Query(value = "SELECT TOP 1 Code FROM material WHERE Code LIKE '1000%' AND ISNUMERIC(Code) = 1 ORDER BY CAST(Code AS INT) DESC", nativeQuery = true)
	String findMaxCodeForModel();

	Optional<Material> findFirstByNameIgnoreCaseAndSpjangcdOrderByIdAsc(String clean, String spjangcd);
}
