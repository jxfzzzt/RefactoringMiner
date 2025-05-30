/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.integration.movies.repo;

import java.util.List;

import org.springframework.data.neo4j.integration.movies.domain.Rating;
import org.springframework.data.neo4j.repository.GraphRepository;

/**
 * @author Luanne Misquitta
 */
public interface RatingRepository extends GraphRepository<Rating> {

	List<Rating> findByStars(int stars);


}
