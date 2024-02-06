
package org.hotswap.agent.plugin.mybatis;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface Mapper {

  @Select("select * from users where name1 = #{name1}")
  User getUser(@Param("name1") String name1);

  User getUserXML(String name);

}
