/*
 * Copyright 2023 the original author or authors.
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

package org.springframework.data.relational.core.mapping;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;
import static org.springframework.data.relational.core.sql.SqlIdentifier.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.util.Streamable;

/**
 * Tests for {@link AggregatePath}.
 *
 * @author Jens Schauder
 * @author Mark Paluch
 */
class AggregatePathUnitTests {
	RelationalMappingContext context = new RelationalMappingContext();

	private RelationalPersistentEntity<?> entity = context.getRequiredPersistentEntity(DummyEntity.class);

	@Test // GH-1525
	void isNotRootForNonRootPath() {

		AggregatePath path = context.getAggregatePath(context.getPersistentPropertyPath("entityId", DummyEntity.class));

		assertThat(path.isRoot()).isFalse();
	}

	@Test // GH-1525
	void isRootForRootPath() {

		AggregatePath path = context.getAggregatePath(entity);

		assertThat(path.isRoot()).isTrue();
	}

	@Test // GH-1525
	void getParentPath() {

		assertSoftly(softly -> {

			softly.assertThat(path("second.third2.value").getParentPath()).isEqualTo(path("second.third2"));
			softly.assertThat(path("second.third2").getParentPath()).isEqualTo(path("second"));
			softly.assertThat(path("second").getParentPath()).isEqualTo(path());

			softly.assertThatThrownBy(() -> path().getParentPath()).isInstanceOf(IllegalStateException.class);
		});
	}

	@Test // GH-1525
	void getRequiredLeafEntity() {

		assertSoftly(softly -> {

			softly.assertThat(path().getRequiredLeafEntity()).isEqualTo(entity);
			softly.assertThat(path("second").getRequiredLeafEntity())
					.isEqualTo(context.getRequiredPersistentEntity(Second.class));
			softly.assertThat(path("second.third").getRequiredLeafEntity())
					.isEqualTo(context.getRequiredPersistentEntity(Third.class));
			softly.assertThat(path("secondList").getRequiredLeafEntity())
					.isEqualTo(context.getRequiredPersistentEntity(Second.class));

			softly.assertThatThrownBy(() -> path("secondList.third.value").getRequiredLeafEntity())
					.isInstanceOf(IllegalStateException.class);

		});
	}

	@Test // GH-1525
	void idDefiningPath() {

		assertSoftly(softly -> {

			softly.assertThat(AggregatePathUtil.getIdDefiningParentPath(path("second.third2.value"))).isEqualTo(path());
			softly.assertThat(AggregatePathUtil.getIdDefiningParentPath(path("second.third.value"))).isEqualTo(path());
			softly.assertThat(AggregatePathUtil.getIdDefiningParentPath(path("secondList.third2.value"))).isEqualTo(path());
			softly.assertThat(AggregatePathUtil.getIdDefiningParentPath(path("secondList.third.value"))).isEqualTo(path());
			softly.assertThat(AggregatePathUtil.getIdDefiningParentPath(path("second2.third2.value"))).isEqualTo(path());
			softly.assertThat(AggregatePathUtil.getIdDefiningParentPath(path("second2.third.value"))).isEqualTo(path());
			softly.assertThat(AggregatePathUtil.getIdDefiningParentPath(path("withId.second.third2.value"))).isEqualTo(path("withId"));
			softly.assertThat(AggregatePathUtil.getIdDefiningParentPath(path("withId.second.third.value"))).isEqualTo(path("withId"));
		});
	}

	@Test // GH-1525
	void getRequiredIdProperty() {

		assertSoftly(softly -> {

			softly.assertThat(path().getRequiredIdProperty().getName()).isEqualTo("entityId");
			softly.assertThat(path("withId").getRequiredIdProperty().getName()).isEqualTo("withIdId");
			softly.assertThatThrownBy(() -> path("second").getRequiredIdProperty()).isInstanceOf(IllegalStateException.class);
		});
	}

	@Test // GH-1525
	void reverseColumnName() {

		assertSoftly(softly -> {

			softly.assertThat(AggregatePathUtil.getReverseColumnName(path("second.third2"))).isEqualTo(quoted("DUMMY_ENTITY"));
			softly.assertThat(AggregatePathUtil.getReverseColumnName(path("second.third"))).isEqualTo(quoted("DUMMY_ENTITY"));
			softly.assertThat(AggregatePathUtil.getReverseColumnName(path("secondList.third2"))).isEqualTo(quoted("DUMMY_ENTITY"));
			softly.assertThat(AggregatePathUtil.getReverseColumnName(path("secondList.third"))).isEqualTo(quoted("DUMMY_ENTITY"));
			softly.assertThat(AggregatePathUtil.getReverseColumnName(path("second2.third2"))).isEqualTo(quoted("DUMMY_ENTITY"));
			softly.assertThat(AggregatePathUtil.getReverseColumnName(path("second2.third"))).isEqualTo(quoted("DUMMY_ENTITY"));
			softly.assertThat(AggregatePathUtil.getReverseColumnName(path("withId.second.third2.value"))).isEqualTo(quoted("WITH_ID"));
			softly.assertThat(AggregatePathUtil.getReverseColumnName(path("withId.second.third"))).isEqualTo(quoted("WITH_ID"));
			softly.assertThat(AggregatePathUtil.getReverseColumnName(path("withId.second2.third"))).isEqualTo(quoted("WITH_ID"));
		});
	}

	@Test // GH-1525
	void getQualifierColumn() {

		assertSoftly(softly -> {

			softly.assertThat(ForeignTableDetector.of(path("secondList")).getQualifierColumn()).isEqualTo(SqlIdentifier.quoted("DUMMY_ENTITY_KEY"));
		});
	}

	@Test // GH-1525
	void getQualifierColumnType() {

		assertSoftly(softly -> {

			softly.assertThat(ForeignTableDetector.of(path("secondList")).getQualifierColumnType()).isEqualTo(Integer.class);
		});
	}

	@Test // GH-1525
	void extendBy() {

		assertSoftly(softly -> {

			softly.assertThat(path().append(entity.getRequiredPersistentProperty("withId"))).isEqualTo(path("withId"));
			softly.assertThat(path("withId").append(path("withId").getRequiredIdProperty()))
					.isEqualTo(path("withId.withIdId"));
		});
	}

	@Test // GH-1525
	void isWritable() {

		assertSoftly(softly -> {
			softly.assertThat(AggregatePathUtil.isWritable(createSimplePath("withId"))).describedAs("simple path is writable")
					.isTrue();
			softly.assertThat(AggregatePathUtil.isWritable(createSimplePath("secondList.third2")))
					.describedAs("long path is writable").isTrue();
			softly.assertThat(AggregatePathUtil.isWritable(createSimplePath("second")))
					.describedAs("simple read only path is not writable").isFalse();
			softly.assertThat(AggregatePathUtil.isWritable(createSimplePath("second.third")))
					.describedAs("long path containing read only element is not writable").isFalse();
		});
	}

	@Test // GH-1525
	void isEmbedded() {

		assertSoftly(softly -> {
			softly.assertThat(path().isEmbedded()).isFalse();
			softly.assertThat(path("withId").isEmbedded()).isFalse();
			softly.assertThat(path("second2.third").isEmbedded()).isFalse();
			softly.assertThat(path("second2").isEmbedded()).isTrue();

		});
	}


	@Test // GH-1525
	void isEntity() {

		assertSoftly(softly -> {

			softly.assertThat(path().isEntity()).isTrue();
			softly.assertThat(path("second").isEntity()).isTrue();
			softly.assertThat(path("second.third2").isEntity()).isTrue();
			softly.assertThat(path("secondList.third2").isEntity()).isTrue();
			softly.assertThat(path("secondList").isEntity()).isTrue();
			softly.assertThat(path("second.third2.value").isEntity()).isFalse();
			softly.assertThat(path("secondList.third2.value").isEntity()).isFalse();
		});
	}

	@Test // GH-1525
	void isMultiValued() {

		assertSoftly(softly -> {

			softly.assertThat(path().isMultiValued()).isFalse();
			softly.assertThat(path("second").isMultiValued()).isFalse();
			softly.assertThat(path("second.third2").isMultiValued()).isFalse();
			softly.assertThat(path("secondList.third2").isMultiValued()).isTrue();
			softly.assertThat(path("secondList").isMultiValued()).isTrue();
		});
	}

	@Test // GH-1525
	void isQualified() {

		assertSoftly(softly -> {

			softly.assertThat(path().isQualified()).isFalse();
			softly.assertThat(path("second").isQualified()).isFalse();
			softly.assertThat(path("second.third2").isQualified()).isFalse();
			softly.assertThat(path("secondList.third2").isQualified()).isFalse();
			softly.assertThat(path("secondList").isQualified()).isTrue();
		});
	}

	@Test // GH-1525
	void isMap() {

		assertSoftly(softly -> {

			softly.assertThat(path().isMap()).isFalse();
			softly.assertThat(path("second").isMap()).isFalse();
			softly.assertThat(path("second.third2").isMap()).isFalse();
			softly.assertThat(path("secondList.third2").isMap()).isFalse();
			softly.assertThat(path("secondList").isMap()).isFalse();
			softly.assertThat(path("secondMap.third2").isMap()).isFalse();
			softly.assertThat(path("secondMap").isMap()).isTrue();
		});
	}

	@Test // GH-1525
	void isCollectionLike() {

		assertSoftly(softly -> {

			softly.assertThat(path().isCollectionLike()).isFalse();
			softly.assertThat(path("second").isCollectionLike()).isFalse();
			softly.assertThat(path("second.third2").isCollectionLike()).isFalse();
			softly.assertThat(path("secondList.third2").isCollectionLike()).isFalse();
			softly.assertThat(path("secondMap.third2").isCollectionLike()).isFalse();
			softly.assertThat(path("secondMap").isCollectionLike()).isFalse();
			softly.assertThat(path("secondList").isCollectionLike()).isTrue();
		});
	}

	@Test // GH-1525
	void isOrdered() {

		assertSoftly(softly -> {

			softly.assertThat(path().isOrdered()).isFalse();
			softly.assertThat(path("second").isOrdered()).isFalse();
			softly.assertThat(path("second.third2").isOrdered()).isFalse();
			softly.assertThat(path("secondList.third2").isOrdered()).isFalse();
			softly.assertThat(path("secondMap.third2").isOrdered()).isFalse();
			softly.assertThat(path("secondMap").isOrdered()).isFalse();
			softly.assertThat(path("secondList").isOrdered()).isTrue();
		});
	}

	@Test // GH-1525
	void getTableAlias() {

		assertSoftly(softly -> {

			softly.assertThat(TableAccessor.of(path()).findTableAlias()).isEqualTo(null);
			softly.assertThat(TableAccessor.of(path("second")).findTableAlias()).isEqualTo(quoted("second"));
			softly.assertThat(TableAccessor.of(path("second.third2")).findTableAlias()).isEqualTo(quoted("second"));
			softly.assertThat(TableAccessor.of(path("second.third2.value")).findTableAlias()).isEqualTo(quoted("second"));
			softly.assertThat(TableAccessor.of(path("second.third")).findTableAlias()).isEqualTo(quoted("second_third"));
			softly.assertThat(TableAccessor.of(path("second.third.value")).findTableAlias()).isEqualTo(quoted("second_third"));
			softly.assertThat(TableAccessor.of(path("secondList.third2")).findTableAlias()).isEqualTo(quoted("secondList"));
			softly.assertThat(TableAccessor.of(path("secondList.third2.value")).findTableAlias()).isEqualTo(quoted("secondList"));
			softly.assertThat(TableAccessor.of(path("secondList.third")).findTableAlias()).isEqualTo(quoted("secondList_third"));
			softly.assertThat(TableAccessor.of(path("secondList.third.value")).findTableAlias()).isEqualTo(quoted("secondList_third"));
			softly.assertThat(TableAccessor.of(path("secondList")).findTableAlias()).isEqualTo(quoted("secondList"));
			softly.assertThat(TableAccessor.of(path("second2.third")).findTableAlias()).isEqualTo(quoted("secthird"));
			softly.assertThat(TableAccessor.of(path("second3.third")).findTableAlias()).isEqualTo(quoted("third"));
		});
	}
	@Test // GH-1525
	void getTableName() {

		assertSoftly(softly -> {

			softly.assertThat(TableAccessor.of(path()).getQualifiedTableName()).isEqualTo(quoted("DUMMY_ENTITY"));
			softly.assertThat(TableAccessor.of(path("second")).getQualifiedTableName()).isEqualTo(quoted("SECOND"));
			softly.assertThat(TableAccessor.of(path("second.third2")).getQualifiedTableName()).isEqualTo(quoted("SECOND"));
			softly.assertThat(TableAccessor.of(path("second.third2.value")).getQualifiedTableName()).isEqualTo(quoted("SECOND"));
			softly.assertThat(TableAccessor.of(path("secondList.third2")).getQualifiedTableName()).isEqualTo(quoted("SECOND"));
			softly.assertThat(TableAccessor.of(path("secondList.third2.value")).getQualifiedTableName()).isEqualTo(quoted("SECOND"));
			softly.assertThat(TableAccessor.of(path("secondList")).getQualifiedTableName()).isEqualTo(quoted("SECOND"));
		});
	}
	@Test // GH-1525
	void getColumnName() {

		assertSoftly(softly -> {

			softly.assertThat(ColumnDetector.of(path("second.third2.value")).getColumnName()).isEqualTo(quoted("THRDVALUE"));
			softly.assertThat(ColumnDetector.of(path("second.third.value")).getColumnName()).isEqualTo(quoted("VALUE"));
			softly.assertThat(ColumnDetector.of(path("secondList.third2.value")).getColumnName()).isEqualTo(quoted("THRDVALUE"));
			softly.assertThat(ColumnDetector.of(path("secondList.third.value")).getColumnName()).isEqualTo(quoted("VALUE"));
			softly.assertThat(ColumnDetector.of(path("second2.third2.value")).getColumnName()).isEqualTo(quoted("SECTHRDVALUE"));
			softly.assertThat(ColumnDetector.of(path("second2.third.value")).getColumnName()).isEqualTo(quoted("VALUE"));
		});
	}

	@Test // GH-1525
	void getColumnAlias() {

		assertSoftly(softly -> {

			softly.assertThat(ColumnDetector.of(path("second.third2.value")).getColumnAlias()).isEqualTo(quoted("SECOND_THRDVALUE"));
			softly.assertThat(ColumnDetector.of(path("second.third.value")).getColumnAlias()).isEqualTo(quoted("SECOND_THIRD_VALUE"));
			softly.assertThat(ColumnDetector.of(path("secondList.third2.value")).getColumnAlias()).isEqualTo(quoted("SECONDLIST_THRDVALUE"));
			softly.assertThat(ColumnDetector.of(path("secondList.third.value")).getColumnAlias()).isEqualTo(quoted("SECONDLIST_THIRD_VALUE"));
			softly.assertThat(ColumnDetector.of(path("second2.third2.value")).getColumnAlias()).isEqualTo(quoted("SECTHRDVALUE"));
			softly.assertThat(ColumnDetector.of(path("second2.third.value")).getColumnAlias()).isEqualTo(quoted("SECTHIRD_VALUE"));
		});
	}

	@Test // GH-1525
	void getReverseColumnAlias() {

		assertSoftly(softly -> {

			softly.assertThat(AggregatePathUtil.getReverseColumnNameAlias(path("second.third2.value"))).isEqualTo(quoted("SECOND_DUMMY_ENTITY"));
			softly.assertThat(AggregatePathUtil.getReverseColumnNameAlias(path("second.third.value"))).isEqualTo(quoted("SECOND_THIRD_DUMMY_ENTITY"));
			softly.assertThat(AggregatePathUtil.getReverseColumnNameAlias(path("secondList.third2.value"))).isEqualTo(quoted("SECONDLIST_DUMMY_ENTITY"));
			softly.assertThat(AggregatePathUtil.getReverseColumnNameAlias(path("secondList.third.value"))).isEqualTo(quoted("SECONDLIST_THIRD_DUMMY_ENTITY"));
			softly.assertThat(AggregatePathUtil.getReverseColumnNameAlias(path("second2.third2.value"))).isEqualTo(quoted("DUMMY_ENTITY"));
			softly.assertThat(AggregatePathUtil.getReverseColumnNameAlias(path("second2.third.value"))).isEqualTo(quoted("SECTHIRD_DUMMY_ENTITY"));
		});
	}

	@Test // GH-1525
	void getRequiredLeafProperty() {

		assertSoftly(softly -> {

			softly.assertThat(path("second.third2.value").getRequiredLeafProperty()).isEqualTo(context.getRequiredPersistentEntity(Third.class).getPersistentProperty("value"));
			softly.assertThat(path("second.third").getRequiredLeafProperty()).isEqualTo(context.getRequiredPersistentEntity(Second.class).getPersistentProperty("third"));
			softly.assertThat(path("secondList").getRequiredLeafProperty()).isEqualTo(entity.getPersistentProperty("secondList"));
			softly.assertThatThrownBy(() -> path().getRequiredLeafProperty()).isInstanceOf(IllegalStateException.class);
		});
	}

	@Test // GH-1525
	void getBaseProperty() {

		assertSoftly(softly -> {

			softly.assertThat(path("second.third2.value").getBaseProperty()).isEqualTo(entity.getPersistentProperty("second"));
			softly.assertThat(path("second.third.value").getBaseProperty()).isEqualTo(entity.getPersistentProperty("second"));
			softly.assertThat(path("secondList.third2.value").getBaseProperty()).isEqualTo(entity.getPersistentProperty("secondList"));
			softly.assertThatThrownBy(() -> path().getBaseProperty()).isInstanceOf(IllegalStateException.class);
		});
	}


	@Test // GH-1525
	void getIdColumnName() {

		assertSoftly(softly -> {

			softly.assertThat(ColumnDetector.of(path()).getIdColumnName()).isEqualTo(quoted("ENTITY_ID"));
			softly.assertThat(ColumnDetector.of(path("withId")).getIdColumnName()).isEqualTo(quoted("WITH_ID_ID"));

			softly.assertThatThrownBy(() -> ColumnDetector.of(path("second")).getIdColumnName()).isInstanceOf(IllegalStateException.class);
			softly.assertThatThrownBy(() ->ColumnDetector.of(path("second.third2")).getIdColumnName()).isInstanceOf(IllegalStateException.class);
			softly.assertThatThrownBy(() ->ColumnDetector.of(path("withId.second")).getIdColumnName()).isInstanceOf(IllegalStateException.class);
		});
	}

	@Test // GH-1525
	void toDotPath() {

		assertSoftly(softly -> {

			softly.assertThat(path().toDotPath()).isEqualTo("");
			softly.assertThat(path("second.third.value").toDotPath()).isEqualTo("second.third.value");
		});
	}

	@Test // GH-1525
	void getRequiredPersistentPropertyPath() {

		assertSoftly(softly -> {

			softly.assertThat(path("second.third.value").getRequiredPersistentPropertyPath()).isEqualTo(createSimplePath("second.third.value"));
			softly.assertThatThrownBy(() -> path().getRequiredPersistentPropertyPath()).isInstanceOf(IllegalStateException.class);
		});
	}

	@Test // GH-1525
	void getEffectiveIdColumnName() {

		assertSoftly(softly -> {

			softly.assertThat(AggregatePathUtil.getEffectiveIdColumnName(path())).isEqualTo(quoted("ENTITY_ID"));
			softly.assertThat(AggregatePathUtil.getEffectiveIdColumnName(path("second.third2"))).isEqualTo(quoted("DUMMY_ENTITY"));
			softly.assertThat(AggregatePathUtil.getEffectiveIdColumnName(path("withId.second.third"))).isEqualTo(quoted("WITH_ID"));
			softly.assertThat(AggregatePathUtil.getEffectiveIdColumnName(path("withId.second.third2.value"))).isEqualTo(quoted("WITH_ID"));
		});
	}

	@Test // GH-1525
	void getLength() {

		assertSoftly(softly -> {

			softly.assertThat(path().getLength()).isEqualTo(0);
			softly.assertThat(path().isRoot()).isTrue();
			softly.assertThat(path("second.third2").getLength()).isEqualTo(2);
			softly.assertThat(path("withId.second.third").getLength()).isEqualTo(3);
			softly.assertThat(path("withId.second.third2.value").getLength()).isEqualTo(4);
		});
	}

	@Test // GH-1525
	void shouldIteratePath() {

		AggregatePath path = path("withId.second.third2");
		List<AggregatePath> aggregatePaths = Streamable.of(path::iterator).stream().toList();

		assertThat(aggregatePaths).hasSize(4).containsExactly(path, path.getParentPath(), path.getParentPath().getParentPath(), path.getParentPath().getParentPath().getParentPath());
	}

	@Test // GH-1525
	void shouldFilterPath() {

		assertThat(path("withId.second.third2").filter(AggregatePath::isRoot)).isEqualTo(path());
		assertThat(path("withId.second.third2").filter(path -> true)).isEqualTo(path("withId.second.third2"));
		assertThat(path("withId.second.third2").filter(path -> false)).isNull();
	}

	private AggregatePath path() {
		return context.getAggregatePath(entity);
	}

	private AggregatePath path(String path) {
		return context.getAggregatePath(createSimplePath(path));
	}

	PersistentPropertyPath<RelationalPersistentProperty> createSimplePath(String path) {
		return PersistentPropertyPathTestUtils.getPath(context, path, DummyEntity.class);
	}

	@SuppressWarnings("unused")
	static class DummyEntity {
		@Id Long entityId;
		@ReadOnlyProperty Second second;
		@Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "sec") Second second2;
		@Embedded(onEmpty = Embedded.OnEmpty.USE_NULL) Second second3;
		List<Second> secondList;
		Map<String, Second> secondMap;
		WithId withId;
	}

	@SuppressWarnings("unused")
	static class Second {
		Third third;
		@Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "thrd") Third third2;
	}

	@SuppressWarnings("unused")
	static class Third {
		String value;
	}

	@SuppressWarnings("unused")
	static class WithId {
		@Id Long withIdId;
		Second second;
		@Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "sec") Second second2;
	}

}
