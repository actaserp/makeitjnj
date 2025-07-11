package mes.domain.repository.actasRepository;

import mes.domain.entity.actasEntity.TB_DA007W;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TB_DA007WRepository extends JpaRepository<TB_DA007W,String> {
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

    void deleteByPk(String custcd, String spjangcd, String reqdate, String reqnum);
}
