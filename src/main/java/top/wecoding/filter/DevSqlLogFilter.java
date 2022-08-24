package top.wecoding.filter;

import com.alibaba.druid.DbType;
import com.alibaba.druid.filter.AutoLoad;
import com.alibaba.druid.filter.FilterEventAdapter;
import com.alibaba.druid.proxy.jdbc.ResultSetProxy;
import com.alibaba.druid.proxy.jdbc.StatementProxy;
import com.alibaba.druid.sql.SQLUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 开发期间打印sql及参数的过滤器
 * 配置druid.proxy-filters: devSqlLogFilter
 * @author ffd
 * @see com.alibaba.druid.filter.FilterEventAdapter
 */
@Slf4j
@AutoLoad
@Component
public class DevSqlLogFilter extends FilterEventAdapter {

    private final ThreadLocal<Long> startTime = new ThreadLocal<>();

    @Override
    protected void statementExecuteUpdateBefore(StatementProxy statement, String sql) {
        commonStar();
        super.statementExecuteUpdateBefore(statement, sql);
    }

    @Override
    protected void statementExecuteUpdateAfter(StatementProxy statement, String sql, int updateCount) {
        commonEnd(sql);
        super.statementExecuteUpdateAfter(statement, sql, updateCount);
    }

    @Override
    protected void statementExecuteQueryBefore(StatementProxy statement, String sql) {
        commonStar();
        super.statementExecuteQueryBefore(statement, sql);
    }

    @Override
    protected void statementExecuteQueryAfter(StatementProxy statement, String sql, ResultSetProxy resultSet) {
        commonEnd(sql);
        super.statementExecuteQueryAfter(statement, sql, resultSet);
    }

    @Override
    protected void statementExecuteBatchBefore(StatementProxy statement) {
        commonStar();
        super.statementExecuteBatchBefore(statement);
    }

    @Override
    protected void statementExecuteBatchAfter(StatementProxy statement, int[] result) {
        commonEnd(statement.getBatchSql());
        super.statementExecuteBatchAfter(statement, result);
    }

    @Override
    protected void statementExecuteBefore(StatementProxy statement, String sql) {
        commonStar();
        super.statementExecuteBefore(statement, sql);
    }

    @Override
    protected void statementExecuteAfter(StatementProxy statement, String sql, boolean result) {
        commonEnd(sql);
        super.statementExecuteAfter(statement, sql, result);
    }


    private void commonStar(){
        startTime.set(System.currentTimeMillis());
    }

    private void commonEnd(String sql){
        long extendTime= System.currentTimeMillis() - startTime.get();
        startTime.remove();
        StringBuilder sb = new StringBuilder();
        sb.append("\n=============sql start=============\n");
//        sb.append(SQLUtils.format(sql, DbType.mysql));
//        sb.append(SQLUtils.format(sql, DbType.oracle));
        sb.append(SQLUtils.format(sql, DbType.postgresql));
        sb.append("\n - > cost time:").append(extendTime).append(" ms");
        sb.append("\n=============sql end=============");
        log.debug(sb.toString());
    }
}