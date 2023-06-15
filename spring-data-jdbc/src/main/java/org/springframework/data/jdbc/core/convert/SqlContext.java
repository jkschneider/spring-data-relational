/*
 * Copyright 2019-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jdbc.core.convert;

import org.springframework.data.relational.core.mapping.AggregatePath;
import org.springframework.data.relational.core.mapping.AggregatePathUtil;
import org.springframework.data.relational.core.mapping.ColumnDetector;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.TableAccessor;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.relational.core.sql.Table;

/**
 * Utility to get from path to SQL DSL elements.
 *
 * @author Jens Schauder
 * @author Mark Paluch
 * @author Tyler Van Gorder
 * @since 1.1
 */
class SqlContext {

	private final RelationalPersistentEntity<?> entity;
	private final Table table;

	SqlContext(RelationalPersistentEntity<?> entity) {

		this.entity = entity;
		this.table = Table.create(entity.getQualifiedTableName());
	}

	Column getIdColumn() {
		return table.column(entity.getIdColumn());
	}

	Column getVersionColumn() {
		return table.column(entity.getRequiredVersionProperty().getColumnName());
	}

	Table getTable() {
		return table;
	}

	Table getTable(AggregatePath path) {
		return getTable(TableAccessor.of(path));
	}

	Table getTable(TableAccessor tableAccessor) {

		SqlIdentifier tableAlias = tableAccessor.findTableAlias();
		Table table = Table.create(tableAccessor.getQualifiedTableName());
		return tableAlias == null ? table : table.as(tableAlias);
	}

	Column getColumn(AggregatePath path) {
		ColumnDetector detector = ColumnDetector.of(path);
		return getTable(path).column(detector.getColumnName()).as(detector.getColumnAlias());
	}

	Column getReverseColumn(AggregatePath path) {
		return getTable(path).column(AggregatePathUtil.getReverseColumnName(path))
				.as(AggregatePathUtil.getReverseColumnNameAlias(path));
	}
}
