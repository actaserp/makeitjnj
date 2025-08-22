package mes.domain.repository.actasRepository;

import mes.domain.entity.actasEntity.TB_XA012;
import mes.domain.entity.actasEntity.TB_XA012ID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TB_XA012Repository extends JpaRepository<TB_XA012, TB_XA012ID> {

    @Query("SELECT t FROM TB_XA012 t WHERE t.id.custcd = :custcd AND t.id.spjangcd IN :spjangcds")
    List<TB_XA012> findByCustcdAndSpjangcds(@Param("custcd") String custcd, @Param("spjangcds") List<String> spjangcds);

    Optional<TB_XA012> findById_Spjangcd(String spjangcd);
}