package mes.domain.repository.actasRepository;

import mes.domain.entity.actasEntity.TB_DA007W;
import mes.domain.entity.actasEntity.TB_DA007W_PK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface TB_DA007WRepository extends JpaRepository<TB_DA007W, TB_DA007W_PK> {
    @Query(value = "SELECT COALESCE(MAX(CAST(t.reqseq AS INT)), 0) FROM TB_DA007W t WHERE " +
            "t.reqnum = :reqnum AND t.custcd = :custcd AND t.spjangcd = :spjangcd", nativeQuery = true)
    int findMaxReqseq(@Param("reqnum") String reqnum
                    , @Param("custcd") String custcd
                    , @Param("spjangcd") String spjangcd);

    @Query(value = "SELECT t.reqseq FROM TB_DA007W t WHERE " +
            "t.reqnum = :reqnum AND t.custcd = :custcd AND t.spjangcd = :spjangcd", nativeQuery = true)
    List<String> findReqseq(@Param("reqnum") String reqnum
                            , @Param("custcd") String custcd
                            , @Param("spjangcd") String spjangcd);

    List<TB_DA007W> findById_CustcdAndId_SpjangcdAndId_ReqdateAndId_Reqnum(String custcd, String spjangcd, String reqdate, String reqnum);

    @Modifying
    @Query("DELETE FROM TB_DA007W d WHERE d.id.custcd = :custcd AND d.id.spjangcd = :spjangcd AND d.id.reqdate = :reqdate AND d.id.reqnum = :reqnum")
    void deleteByPk(@Param("custcd") String custcd,
                    @Param("spjangcd") String spjangcd,
                    @Param("reqdate") String reqdate,
                    @Param("reqnum") String reqnum);

}
