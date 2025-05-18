package mes.app.system.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import mes.domain.entity.User;
import mes.domain.entity.UserGroup;
import mes.domain.services.SqlRunner;

@Repository
public class SystemService {

    @Autowired
    SqlRunner sqlRunner;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    // 권한별 메뉴 리스트
    public List<Map<String, Object>> getWebMenuList(User user) {

        String sql = """
                 with recursive_tree(id, menu_code, pid, name, depth, path, cycle, folder_order, _order, css, data_div, folder_id, FrontFolder_id) as (
                    SELECT
                        a.id,
                        CAST(NULL AS NVARCHAR(MAX)) AS menu_code,
                        a.Parent_id AS pid,
                        a.FolderName AS name,
                        1 AS depth,
                        CAST('/' + CAST(a.id AS NVARCHAR(MAX)) AS NVARCHAR(MAX)) AS path, 
                        0 AS cycle,
                        a._order AS folder_order,
                        a._order AS _order,
                        a.IconCSS AS css, 
                        CAST('folder' AS NVARCHAR(10)) AS data_div, 
                        a.id AS folder_id,
                        a.FrontFolder_id
                    FROM
                        menu_folder a
                    WHERE
                        a.Parent_id IS NULL
                        
                    UNION ALL
                        
                   
                    SELECT
                         NULL AS id,
                         CAST(mi.MenuCode AS NVARCHAR(MAX)) AS menu_code,
                         mi.MenuFolder_id AS pid,
                         mi.MenuName AS name,
                         recursive_tree.depth + 1 AS depth,
                         recursive_tree.path + '/' + CAST(mi.MenuFolder_id AS NVARCHAR(MAX)) AS path, 
                         CASE WHEN mi.MenuFolder_id IN (SELECT splitdata FROM dbo.SplitStrings(recursive_tree.path, '/')) THEN 1 ELSE 0 END AS cycle, 
                         recursive_tree.folder_order AS folder_order,
                         mi._order AS _order,
                         NULL AS css,
                         CAST('menu' AS NVARCHAR(10)) AS data_div,  
                         mi.MenuFolder_id AS folder_id,
                         recursive_tree.FrontFolder_id
                    FROM
                        menu_item mi
                    INNER JOIN
                        recursive_tree ON mi.MenuFolder_id = recursive_tree.id
                    WHERE
                        (:super_user = 1)
                        OR EXISTS (
                            SELECT 1
                            FROM user_group_menu gm
                            WHERE gm.MenuCode = mi.MenuCode
                            AND gm.UserGroup_id = :group_id
                            AND (gm.AuthCode LIKE '%R%' OR gm.AuthCode LIKE '%W%')
                        )
                        AND cycle = 0
                ),
                M AS (
                    SELECT
                         recursive_tree.id,
                         recursive_tree.menu_code,
                         recursive_tree.pid,
                         recursive_tree.name,
                         recursive_tree.depth,
                         recursive_tree.folder_order,
                         recursive_tree._order,
                         ISNULL(recursive_tree.css, '') AS css,
                         CASE WHEN bk.MenuCode IS NOT NULL THEN 1 ELSE 0 END AS isbookmark,
                         recursive_tree.data_div,
                         COUNT(*) OVER (PARTITION BY recursive_tree.folder_id) AS sub_count,
                         recursive_tree.path,
                         recursive_tree.FrontFolder_id
                    FROM
                        recursive_tree
                    LEFT JOIN
                        bookmark bk ON bk.MenuCode = recursive_tree.menu_code
                        AND bk.User_id = :user_id
                    WHERE
                        1 = 1
                )
                SELECT
                    id,
                    menu_code,
                    pid,
                    name,
                    depth,
                    folder_order,
                    _order,
                    css,
                    isbookmark,
                    CASE WHEN depth = 1 THEN FrontFolder_id ELSE NULL END AS FrontFolder_id
                FROM
                    M
                WHERE
                    sub_count > 1
                AND
                    FrontFolder_id IS NOT NULL
                ORDER BY
                    depth, _order;
        """;

        UserGroup userGroup = user.getUserProfile().getUserGroup();

        String group_code = userGroup.getCode();
        boolean super_user = user.getSuperUser();

        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("group_id", userGroup.getId());
        dicParam.addValue("user_id", user.getId());
        dicParam.addValue("group_code", group_code);
        dicParam.addValue("super_user", super_user);
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);

        return items;

    }

    /**
     * 시스템-메뉴권한 : 사용자그룹별 메뉴조회
     *
     * @param userGroupId
     * @param folderId
     * @return
     */
    public List<Map<String, Object>> getUserGroupMenuList1(Integer userGroupId, Integer folderId) {
        System.out.println("Folder ID: " + folderId);
        System.out.println("User ID: " + userGroupId);
        String sql = """
    WITH tree AS (
        SELECT 
            a.id,
            a.Parent_id AS pid,
            CAST('' AS NVARCHAR(MAX)) AS menu_code,
            a.FolderName AS gpname,
            a.FolderName AS name,
            1 AS depth,
            CAST(a.id AS NVARCHAR(MAX)) AS path,
            0 AS cycle,
            a._order AS ord,
            CAST('folder' AS NVARCHAR(10)) AS data_div,
            a.id AS folder_id,
            1 AS is_folder
        FROM menu_folder a
        WHERE a.Parent_id IS NULL
          AND a.FrontFolder_id IS NOT NULL
    """;

        if (folderId != null) {
            sql += " and a.id = :folder_id";
        }

        sql += """
        UNION ALL
        SELECT 
            NULL AS id,
            mi.MenuFolder_id AS pid,
            CAST(mi.MenuCode AS NVARCHAR(MAX)) AS menu_code,
            tree.gpname AS gpname,
            mi.MenuName AS name,
            tree.depth + 1,
            tree.path + '/' + CAST(mi.MenuFolder_id AS NVARCHAR(MAX)) AS path,
            CASE WHEN CHARINDEX(CAST(mi.MenuFolder_id AS NVARCHAR(MAX)), tree.path) > 0 THEN 1 ELSE 0 END AS cycle,
            mi._order AS ord,
            CAST('menu' AS NVARCHAR(10)) AS data_div,
            mi.MenuFolder_id AS folder_id,
            0 AS is_folder
        FROM menu_item mi
        INNER JOIN tree ON mi.MenuFolder_id = tree.id
        WHERE mi.MenuCode NOT IN ('wm_user_group_menu')
    )
    SELECT 
        tree.pid,
        tree.id,
        tree.menu_code,
        tree.gpname,
        tree.name,
        tree.depth,
        tree.ord,
        ugm.UserGroup_id,
        ugm.AuthCode,
        CASE WHEN tree.is_folder = 1 THEN NULL ELSE IIF(CHARINDEX('R', ugm.AuthCode) > 0, 1, 0) END AS r,
        CASE WHEN tree.is_folder = 1 THEN NULL ELSE IIF(CHARINDEX('W', ugm.AuthCode) > 0, 1, 0) END AS w,
        CASE WHEN tree.is_folder = 1 THEN NULL ELSE IIF(CHARINDEX('X', ugm.AuthCode) > 0, 1, 0) END AS x,
        tree.is_folder,
        ugm.id AS ugm_id
    FROM tree 
    LEFT JOIN user_group_menu ugm 
        ON ugm.MenuCode = tree.menu_code 
        AND ugm.UserGroup_id = :group_id
    ORDER BY tree.path, tree.ord
    """;

        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("folder_id", folderId);
        dicParam.addValue("group_id", userGroupId);

        //System.out.println("Executing SQL: " + sql);
        return this.sqlRunner.getRows(sql, dicParam);
    }

    public List<Map<String, Object>> getUserGroupMenuList(Integer userGroupId, Integer folderId) {
        System.out.println("Folder ID: " + folderId);
        System.out.println("User ID: " + userGroupId);
        String sql = """
    WITH tree AS (
        -- 루트 폴더 데이터
        SELECT 
            a.id,
            a.Parent_id AS pid,
            CAST('' AS NVARCHAR(MAX)) AS menu_code,
            a.FolderName AS gpname,
            a.FolderName AS name,
            1 AS depth,
            CAST(a.id AS NVARCHAR(MAX)) AS path,
            0 AS cycle,
            a._order AS ord,
            CAST('folder' AS NVARCHAR(10)) AS data_div,
            a.id AS folder_id,
            1 AS is_folder
        FROM menu_folder a
        WHERE a.Parent_id IS NULL
          AND (a.FrontFolder_id IS NOT NULL OR a.FrontFolder_id IS NULL)
    """;

        // Optional folder ID filtering
        if (folderId != null) {
            sql += " AND a.id = :folder_id";
        }

        sql += """
        UNION ALL
        -- 하위 메뉴 데이터
        SELECT 
            NULL AS id,
            mi.MenuFolder_id AS pid,
            CAST(mi.MenuCode AS NVARCHAR(MAX)) AS menu_code,
            tree.gpname AS gpname,
            mi.MenuName AS name,
            tree.depth + 1,
            tree.path + '/' + CAST(mi.MenuFolder_id AS NVARCHAR(MAX)) AS path,
            CASE WHEN CHARINDEX(CAST(mi.MenuFolder_id AS NVARCHAR(MAX)), tree.path) > 0 THEN 1 ELSE 0 END AS cycle,
            mi._order AS ord,
            CAST('menu' AS NVARCHAR(10)) AS data_div,
            mi.MenuFolder_id AS folder_id,
            0 AS is_folder
        FROM menu_item mi
        INNER JOIN tree ON mi.MenuFolder_id = tree.id
        WHERE mi.MenuCode NOT IN ('wm_user_group_menu')
    )
    SELECT 
        tree.pid,
        tree.id,
        tree.menu_code,
        tree.gpname,
        tree.name,
        tree.depth,
        tree.ord,
        ugm.UserGroup_id,
        ugm.AuthCode,
        CASE 
            WHEN tree.is_folder = 1 THEN NULL
            ELSE IIF(ugm.AuthCode IS NULL, 0, IIF(CHARINDEX('R', ugm.AuthCode) > 0, 1, 0))
        END AS r,
        CASE 
            WHEN tree.is_folder = 1 THEN NULL
            ELSE IIF(ugm.AuthCode IS NULL, 0, IIF(CHARINDEX('W', ugm.AuthCode) > 0, 1, 0))
        END AS w,
        CASE 
            WHEN tree.is_folder = 1 THEN NULL
            ELSE IIF(ugm.AuthCode IS NULL, 0, IIF(CHARINDEX('X', ugm.AuthCode) > 0, 1, 0))
        END AS x,
        tree.is_folder,
        ugm.id AS ugm_id
    FROM tree 
    LEFT JOIN user_group_menu ugm 
        ON ugm.MenuCode = tree.menu_code 
        AND ugm.UserGroup_id = :group_id
    ORDER BY tree.path, tree.ord
    """;

        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("folder_id", folderId);
        dicParam.addValue("group_id", userGroupId);
//        System.out.println("Executing SQL: " + sql + " with folder_id=" + folderId);

//        System.out.println("Executing SQL: " + sql);
        return this.sqlRunner.getRows(sql, dicParam);
    }

    public List<Map<String, Object>> getBookmarkList(int userId) {

        String sql = """
                      select mi.MenuName as name 
                      , mi.MenuCode as code 
                      , mi.Url as url
                      --, false as ismanual
                      -- , 0 as ismanual
                      , bm._created as created
                      from bookmark bm 
                      inner join menu_item mi on bm.MenuCode = mi.MenuCode 
                      where bm.User_id = :user_id
                      order by created
                """;

        MapSqlParameterSource dicParam = new MapSqlParameterSource();

        dicParam.addValue("user_id", userId);
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
        return items;
    }

    public int saveBookmark(String menucode, String isbookmark, User user) {
        int iRowEffected = 0;

        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        namedParameters.addValue("menucode", menucode);
        namedParameters.addValue("user_id", user.getId());

        if ("false".equals(isbookmark)) {
            // isbookmark가 'false'일 경우, 북마크 추가
            String sql = """
                insert into bookmark ("User_id", "MenuCode", _created) values(:user_id, :menucode, now())
                """;
            iRowEffected = this.jdbcTemplate.update(sql, namedParameters);
        } else {
            // isbookmark가 'true'일 경우, 북마크 삭제
            String sql = """
                delete from bookmark where "User_id"=:user_id and "MenuCode"=:menucode
                """;
            iRowEffected = this.jdbcTemplate.update(sql, namedParameters);
        }

        return iRowEffected;
    }

    public List<Map<String, Object>> getLabelList(String lang_code, String gui_code, String template_key) {

        String sql = """
                select
                lc.ModuleName as gui_code
                , lc.TemplateKey as template_key
                , lc.LabelCode as label_code
                , lc.Description as descr
                , lcl.DispText as text
                from label_code lc 
                left join label_code_lang lcl on lc.id = lcl.LabelCode_id 
                where lcl.LangCode = :lang_code 
                """;
        if (StringUtils.hasText(gui_code)) {
            sql += """	            		
                    and lc.ModuleName = :gui_code and lc.TemplateKey=:template_key 
                    """;
        }
        sql += """ 
                        order by lc.ModuleName, lc.TemplateKey 
                """;

        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("lang_code", lang_code);
        dicParam.addValue("gui_code", gui_code);
        dicParam.addValue("template_key", template_key);
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);
        return items;

    }


    public List<Map<String, Object>> getGridColumnList(String moduleName, String templateName, String gridName, String langCode) {

        String sql = """
                          select gc.Index as index2, gc.Key as key
                          , gc.Label as src_label
                          , gc.Width as width
                          , case when gc.Hidden = 'Y' then 'true' else 'false' end as hidden
                       , coalesce(cl.DispText, gc.Label) as label
                       , cl.DispText as text
                       from grid_col gc 
                       left join grid_col_lang cl on cl.GridColumn_id = gc.id
                       and cl.LangCode = :lang_code
                       where gc.ModuleName = :module_name
                       and gc.TemplateKey = :template_name
                       and gc.GridName = :grid_name
                       order by gc.Index
                """;

        MapSqlParameterSource dicParam = new MapSqlParameterSource();
        dicParam.addValue("module_name", moduleName);
        dicParam.addValue("template_name", templateName);
        dicParam.addValue("grid_name", gridName);
        dicParam.addValue("lang_code", langCode);
        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, dicParam);

        return items;
    }

    public List<Map<String, Object>> getSystemLogList(Timestamp start, Timestamp end, String type, String source) {
        String sql = """
           SELECT 
               ROW_NUMBER() OVER (ORDER BY _created DESC) AS row_num,
               id,
               Type AS type,
               Source AS source,
               Message AS message,
               FORMAT(_created, 'yyyy-MM-dd HH:mm:ss') AS created
           FROM sys_log sl
           WHERE _created BETWEEN :start AND :end
        """;

        if (StringUtils.hasText(type)) {
            sql += """
            AND Type LIKE '%' + :type + '%'
            """;
        }

        if (StringUtils.hasText(source)) {
            sql += """
            AND Source LIKE '%' + :source + '%'		
            """;
        }

        sql += " ORDER BY _created DESC";

        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        namedParameters.addValue("start", start, java.sql.Types.TIMESTAMP);
        namedParameters.addValue("end", end, java.sql.Types.TIMESTAMP);
        namedParameters.addValue("type", type);
        namedParameters.addValue("source", source);

        return this.jdbcTemplate.queryForList(sql, namedParameters);
    }


    public Map<String, Object> getSystemLogDetail(Long id) {
        String sql = """
                       select id, Type as type, Source as source,Message as message
                       , FORMAT(_created ,'yyyy-mm-dd hh24:mi:ss') as created
                       from sys_log sl
                       where id = :log_id    			
                """;
        MapSqlParameterSource namedParameters = new MapSqlParameterSource();
        namedParameters.addValue("log_id", id);

        return this.jdbcTemplate.queryForMap(sql, namedParameters);
    }

//    @Cacheable(value="grid_column",key="{#moduleName, #templateName, #gridName, #key}")
//    public Map<String, Object> getGridColums(String moduleName, String templateName, String gridName, String key){
//
//    	String sql = """
//        select  id, "ModuleName", "TemplateKey", "GridName", "Key", "Index", "Label", "Width", "Hidden"
//        , _status, _created, _modified, _creater_id, _modifier_id
//        from public.grid_col
//    	where  "ModuleName" = :moduleName and "TemplateKey" = :templateName and "GridName" = :gridName and "Key" = :key
//    	""";
//
//    	MapSqlParameterSource namedParameters = new MapSqlParameterSource();
//    	namedParameters.addValue("moduleName", moduleName);
//    	namedParameters.addValue("templateName", templateName);
//    	namedParameters.addValue("gridName", gridName);
//    	namedParameters.addValue("key", key);
//
//    	Map<String, Object> items = this.sqlRunner.getRow(sql, namedParameters);
//    	//return this.jdbcTemplate.queryForObject(sql, namedParameters, GridColumn.class);
//    	return items;
//    }

//    @CachePut(value = "grid_column",key="{#gridColumn.moduleName, #gridColumn.templateName, #gridColumn.gridName, #key}")
//    public GridColumn saveGridColumn(GridColumn gridColumn) {
//
//    	MapSqlParameterSource namedParameters = new MapSqlParameterSource();
//    	namedParameters.addValue("id", gridColumn.getId());
//		namedParameters.addValue("ModuleName", gridColumn.getModuleName());
//		namedParameters.addValue("TemplateKey", gridColumn.getTemplateKey());
//		namedParameters.addValue("GridName", gridColumn.getGridName());
//		namedParameters.addValue("Key", gridColumn.getKey());
//		namedParameters.addValue("Index", gridColumn.getIndex());
//		namedParameters.addValue("Label", gridColumn.getLabel());
//		namedParameters.addValue("Width", gridColumn.getWidth());
//		namedParameters.addValue(":Hidden", gridColumn.getHidden());
//
//		namedParameters.addValue(":_status", gridColumn.get_status());
//		namedParameters.addValue(":_created", gridColumn.get_created());
//		namedParameters.addValue(":_modified", gridColumn.get_modified());
//		namedParameters.addValue(":_creater_id", gridColumn.get_creater_id());
//		namedParameters.addValue(":_modifier_id", gridColumn.get_modifier_id());
//
//    	if(gridColumn.getId()==null) {
//
//    		String sql = """
//            INSERT INTO grid_col("ModuleName", "TemplateKey", "GridName", "Key", "Index", "Label", "Width", "Hidden", "_status", "_created", "_modified", "_creater_id", "_modifier_id",)
//            VALUES(
//            :ModuleName
//            , :TemplateKey
//            , :GridName
//            , :Key
//            , :Index
//            , :Label
//            , :Width
//            , :Hidden
//            , :_status
//            , :_created
//            , :_modified
//            , :_creater_id
//            , :_modifier_id
//            )
//            """;
//
//    		KeyHolder keyHolder = new GeneratedKeyHolder();
//        	this.jdbcTemplate.update(sql, namedParameters, keyHolder, new String[] {"id"});
//        	Integer id = keyHolder.getKey().intValue();
//        	gridColumn.setId(id);
//
//    	}else {
//    		String sql = """
//            UPDATE grid_col SET
//            "_status"=:_status
//            , "_created"=:_created
//            , "_modified"=:_modified
//            , "_creater_id"=:_creater_id
//            , "_modifier_id"=:_modifier_id
//            , "ModuleName"=:ModuleName
//            , "TemplateKey"=:TemplateKey
//            , "GridName"=:GridName
//            , "Key"=:Key
//            , "Index"=:Index
//            , "Label"=:Label
//            , "Width"=:Width
//            , "Hidden"=:Hidden
//            WHERE id=:id
//    		""";
//
//    		this.jdbcTemplate.update(sql, namedParameters);
//    	}
//    	return gridColumn;
//    }

//    @CacheEvict(value = "grid_column", key="{#moduleName, #templateName, #gridName}")
//    public int deleteGridColumns(String moduleName, String templateName, String gridName) {
//    	int iRowEffected = 0;
//
//    	String sql = """
//    	delete from grid_col where "ModuleName"=:ModuleName and "TemplateKey"=:TemplateKey and "GridName"=:GridName
//    	""";
//
//    	MapSqlParameterSource namedParameters = new MapSqlParameterSource();
//    	namedParameters.addValue("ModuleName", moduleName);
//    	namedParameters.addValue("TemplateKey", templateName);
//    	namedParameters.addValue("GridName", gridName);
//
//    	iRowEffected = this.jdbcTemplate.update(sql, namedParameters);
//    	return iRowEffected;
//    }

    public Map<String, Object> getLabelCodeLangDetail(String guiCode, String labelCode, String langCode,
                                                      String templateKey) {

        MapSqlParameterSource paramMap = new MapSqlParameterSource();
        paramMap.addValue("guiCode", guiCode);
        paramMap.addValue("labelCode", labelCode);
        paramMap.addValue("langCode", langCode);
        paramMap.addValue("templateKey", templateKey);

        String sql = """
                select lc.id as lable_code_id
                            , lcl.id as label_lang_id
                            , lc.ModuleName
                            , coalesce ((select MenuName from menu_item where MenuCode=lc.ModuleName limit 1), 'Common') as menu_name
                            , lc.TemplateKey
                            , lc.LabelCode
                            , lc.Description
                            , lcl.LangCode
                            , lcl.DispText
                            , FORMAT(lcl._created ,'yyyy-mm-dd hh24:mi:ss') as disp_created
                            from label_code_lang lcl
                            inner join label_code lc on lcl.LabelCode_id = lc.id
                            where lcl.LangCode = :langCode
                            and lc.ModuleName = :guiCode
                            and lc.TemplateKey = :templateKey
                            and lc.LabelCode = :labelCode
                """;

        Map<String, Object> item = this.sqlRunner.getRow(sql, paramMap);
        return item;
    }

    public List<Map<String, Object>> storyBoard() {
        String sql = """
                select si.id
                        , case when si.BoardType ='menu' 
                            then concat(mi.MenuName,'(', mf.FolderName,')')
                           else df.FormName end as name
                          , si.BoardType
                          , dbo.fn_code_name('story_board_type', si.BoardType ) as BoardTypeName
                          , si.Duration
                          , si.Url
                          , up.Name as writer
                          , FORMAT(si._created ,'yyyy-mm-dd hh24:mi:ss') as created
                          , si.ParameterData
                         from storyboard_item si 
                         left join menu_item mi on mi.MenuCode = si.MenuCode 
                         left join menu_folder mf on mf.id = mi.MenuFolder_id 
                         left join doc_form df on df.id = si.ParameterData::int
                         left join user_profile up on up.User_id = si._creater_id   
                         order by id  
                """;

        List<Map<String, Object>> items = this.sqlRunner.getRows(sql, null);

        return items;
    }
}