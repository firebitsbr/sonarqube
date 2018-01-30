package org.sonar.server.platform.db.migration.version.v71;

import java.sql.SQLException;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.RuleScope;
import org.sonar.api.utils.System2;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class SetRuleScopeToMainTest {
  @Rule
  public final CoreDbTester dbTester = CoreDbTester.createForSchema(SetRuleScopeToMainTest.class, "rules.sql");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private System2 system = new System2();

  private SetRuleScopeToMain underTest = new SetRuleScopeToMain(dbTester.database(), system);

  @Test
  public void has_no_effect_if_table_rules_is_empty() throws SQLException {
    underTest.execute();

    assertThat(dbTester.countRowsOfTable("rules")).isEqualTo(0);
  }

  @Test
  public void updates_rows_with_null_is_build_in_column_to_false() throws SQLException {
    insertRow(1, null);
    insertRow(2, null);

    assertThat(countRowsWithValue(null)).isEqualTo(2);
    assertThat(countRowsWithValue(RuleScope.MAIN)).isEqualTo(0);

    underTest.execute();

    assertThat(countRowsWithValue(null)).isEqualTo(0);
    assertThat(countRowsWithValue(RuleScope.MAIN)).isEqualTo(2);
  }

  @Test
  public void support_large_number_of_rows() throws SQLException {
    for (int i = 0; i < 2_000; i++) {
      insertRow(i, null);
    }

    assertThat(countRowsWithValue(null)).isEqualTo(2000);
    assertThat(countRowsWithValue(RuleScope.MAIN)).isZero();

    underTest.execute();

    assertThat(countRowsWithValue(RuleScope.MAIN)).isEqualTo(2_000);
    assertThat(countRowsWithValue(null)).isEqualTo(0);
  }

  @Test
  public void execute_is_reentreant() throws SQLException {
    insertRow(1, null);
    insertRow(2, RuleScope.MAIN);

    underTest.execute();

    underTest.execute();

    assertThat(countRowsWithValue(null)).isEqualTo(0);
    assertThat(countRowsWithValue(RuleScope.MAIN)).isEqualTo(2);
  }

  private int countRowsWithValue(@Nullable RuleScope value) {
    if (value == null) {
      return dbTester.countSql("select count(1) from rules where scope is null");
    }
    return dbTester.countSql("select count(1) from rules where scope='" + value + "'");
  }

  private void insertRow(int id, @Nullable RuleScope scope) {
    dbTester.executeInsert(
      "RULES",
      "PLUGIN_RULE_KEY", "key_" + id,
      "PLUGIN_NAME", "name_" + id,
      "SCOPE", scope == null ? null : scope.name());
  }
}
