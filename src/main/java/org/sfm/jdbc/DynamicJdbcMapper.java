package org.sfm.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.sfm.map.FieldMapperErrorHandler;
import org.sfm.map.Mapper;
import org.sfm.map.MapperBuilderErrorHandler;
import org.sfm.reflect.Instantiator;
import org.sfm.reflect.Setter;
import org.sfm.reflect.SetterFactory;
import org.sfm.utils.Handler;
import org.sfm.utils.ListHandler;

public final class DynamicJdbcMapper<T> implements JdbcMapper<T> {

	private final Map<String, Setter<T, Object>> setters;
	
	private final SetterFactory setterFactory;
	private final Instantiator<T> instantiator;
	private final Class<T> target;
	
	@SuppressWarnings("unchecked")
	private final AtomicReference<CacheEntry<T>[]> mapperCache = new AtomicReference<CacheEntry<T>[]>(new CacheEntry[0]);

	private FieldMapperErrorHandler fieldMapperErrorHandler;

	private MapperBuilderErrorHandler mapperBuilderErrorHandler;

	public DynamicJdbcMapper(final Class<T> target, final SetterFactory setterFactory, 
			final Instantiator<T> instantiator, final FieldMapperErrorHandler fieldMapperErrorHandler, 
			final MapperBuilderErrorHandler mapperBuilderErrorHandler) {
		this.setterFactory = setterFactory;
		this.setters = setterFactory.getAllSetters(target);
		this.instantiator = instantiator;
		this.target = target;
		this.fieldMapperErrorHandler = fieldMapperErrorHandler;
		this.mapperBuilderErrorHandler = mapperBuilderErrorHandler;
	}
	
	private static final class CacheEntry<T> {
		final MapperKey key;
		final Mapper<ResultSet, T> mapper;
		public CacheEntry(final MapperKey key, final Mapper<ResultSet, T> mapper) {
			this.key = key;
			this.mapper = mapper;
		}
	}


	@Override
	public final void map(final ResultSet source, final T target) throws Exception {
		final Mapper<ResultSet, T> mapper = buildMapper(source.getMetaData());
		mapper.map(source, target);
	}

	@Override
	public final <H extends Handler<T>> H forEach(final ResultSet rs, final H handle)
			throws Exception {
		final Mapper<ResultSet, T> mapper = buildMapper(rs.getMetaData());
		forEach(rs, handle, mapper);
		return handle;
	}

	private <H extends Handler<T>> void forEach(final ResultSet rs,	final H handle, final Mapper<ResultSet, T> mapper)
			throws SQLException, Exception {
		while(rs.next()) {
			final T t = instantiator.newInstance();
			mapper.map(rs, t);
			handle.handle(t);
		}
	}
	
	@Override
	public <H extends Handler<T>> H forEach(final ResultSet rs, final H handle, final T t)
			throws Exception {
		final Mapper<ResultSet, T> mapper = buildMapper(rs.getMetaData());
		while(rs.next()) {
			mapper.map(rs, t);
			handle.handle(t);
		}
		return handle;
	}

	@Override
	public <H extends Handler<T>> H forEach(final PreparedStatement statement, final H handle)
			throws Exception {
		final ResultSet rs = statement.executeQuery();
		try {
			forEach(rs, handle);
		} finally {
			rs.close();
		}
		return handle;
	}

	private Mapper<ResultSet, T> buildMapper(final ResultSetMetaData metaData) throws SQLException {
		
		final MapperKey key = MapperKey.valueOf(metaData);
		
		Mapper<ResultSet, T> mapper = getMapper(key);
		
		if (mapper == null) {
			final CachedResultSetMapperBuilder<T> builder = new CachedResultSetMapperBuilder<T>(target, setters, setterFactory);
			
			builder.fieldMapperErrorHandler(fieldMapperErrorHandler);
			builder.mapperBuilderErrorHandler(mapperBuilderErrorHandler);
			builder.addMapping(metaData);
			
			mapper = builder.mapper();
			
			addToMapperCache(new CacheEntry<>(key, mapper));
		}
		return mapper;
	}
	
	@SuppressWarnings("unchecked")
	private void addToMapperCache(final CacheEntry<T> cacheEntry) {
		CacheEntry<T>[] entries;
		CacheEntry<T>[] newEntries;
		do {
			entries = mapperCache.get();
			
			for(int i = 0; i < entries.length; i++) {
				if (entries[0].key.equals(cacheEntry.key)) {
					// already added 
					return;
				}
			}
			
			newEntries = new CacheEntry[entries.length + 1];
			
			System.arraycopy(entries, 0, newEntries, 0, entries.length);
			newEntries[entries.length] = cacheEntry;
		
		} while(!mapperCache.compareAndSet(entries, newEntries));
	}

	protected Mapper<ResultSet, T> getMapper(MapperKey key) {
		final CacheEntry<T>[] entries = mapperCache.get();
		for(int i = 0; i < entries.length; i++) {
			final CacheEntry<T> entry = entries[i];
			if (entry.key.equals(key)) {
				return entry.mapper;
			}
		}
		return null;
	}

	@Override
	public List<T> list(final ResultSet rs) throws Exception {
		return forEach(rs, new ListHandler<T>()).getList();
	}

	@Override
	public List<T> list(final PreparedStatement ps) throws Exception {
		return forEach(ps, new ListHandler<T>()).getList();
	}
}
