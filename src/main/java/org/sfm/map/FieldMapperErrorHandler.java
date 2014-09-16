package org.sfm.map;

public interface FieldMapperErrorHandler<K> {
	void errorMappingField(K key, Object source, Object target, Exception error) throws MappingException;
}
