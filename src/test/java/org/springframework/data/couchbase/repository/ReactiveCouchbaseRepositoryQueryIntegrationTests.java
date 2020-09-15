/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.data.couchbase.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.couchbase.CouchbaseClientFactory;
import org.springframework.data.couchbase.config.AbstractCouchbaseConfiguration;
import org.springframework.data.couchbase.domain.Airport;
import org.springframework.data.couchbase.domain.ReactiveAirportRepository;
import org.springframework.data.couchbase.repository.config.EnableReactiveCouchbaseRepositories;
import org.springframework.data.couchbase.util.Capabilities;
import org.springframework.data.couchbase.util.ClusterAwareIntegrationTests;
import org.springframework.data.couchbase.util.ClusterType;
import org.springframework.data.couchbase.util.IgnoreWhen;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.couchbase.client.core.error.IndexExistsException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * template class for Reactive Couchbase operations
 *
 * @author Michael Nitschinger
 * @author Michael Reiche
 */
@SpringJUnitConfig(ReactiveCouchbaseRepositoryQueryIntegrationTests.Config.class)
@IgnoreWhen(missesCapabilities = Capabilities.QUERY, clusterTypes = ClusterType.MOCKED)
public class ReactiveCouchbaseRepositoryQueryIntegrationTests extends ClusterAwareIntegrationTests {

	@Autowired CouchbaseClientFactory clientFactory;

	@Autowired ReactiveAirportRepository airportRepository; // intellij flags "Could not Autowire", but it runs ok.

	@BeforeEach
	void beforeEach() {
		try {
			clientFactory.getCluster().queryIndexes().createPrimaryIndex(bucketName());
		} catch (IndexExistsException ex) {
			// ignore, all good.
		}
	}

	@Test
	void shouldSaveAndFindAll() {
		Airport vie = null;
		try {
			vie = new Airport("airports::vie", "vie", "loww");
			airportRepository.save(vie).block();

			List<Airport> all = airportRepository.findAll().toStream().collect(Collectors.toList());

			assertFalse(all.isEmpty());
			assertTrue(all.stream().anyMatch(a -> a.getId().equals("airports::vie")));
		} finally {
			airportRepository.delete(vie).block();
		}
	}

	@Test
	void findBySimpleProperty() {
		Airport vie = null;
		try {
			vie = new Airport("airports::vie", "vie", "loww");
			vie = airportRepository.save(vie).block();
			List<Airport> airports = airportRepository.findAllByIata("vie").collectList().block();
			assertEquals(1, airports.size());
			System.out.println("findAllByIata(0): " + airports.get(0));
			Airport airport1 = airportRepository.findById(airports.get(0).getId()).block();
			assertEquals(airport1.getIata(), vie.getIata());
			System.out.println("findById: " + airport1);
			Airport airport2 = airportRepository.findByIata(airports.get(0).getIata()).block();
			assertEquals(airport1.getId(), vie.getId());
			System.out.println("findByIata: " + airport2);
		} finally {
			airportRepository.delete(vie).block();
		}
	}

	@Test
	void count() {
		String[] iatas = { "JFK", "IAD", "SFO", "SJC", "SEA", "LAX", "PHX" };
		Future[] future = new Future[iatas.length];
		ExecutorService executorService = Executors.newFixedThreadPool(iatas.length);
		try {
			Callable<Boolean>[] suppliers = new Callable[iatas.length];
			for (int i = 0; i < iatas.length; i++) {
				Airport airport = new Airport("airports::" + iatas[i], iatas[i] /*iata*/, iatas[i].toLowerCase() /* lcao */);
				airportRepository.save(airport).block();
			}

			int page = 0;
			for (; page < 10;) {
				PageRequest pageable = PageRequest.of(page++, 2);
				Flux<Airport> airportFlux = airportRepository.findAllByIataLike("S%", pageable);
				List<Airport> airportList = airportFlux.collectList().block();
				if (airportList.isEmpty()) {
					break;
				}
				for (Airport a : airportList) {
					System.out.println(a.getIata() + " " + a.getIcao());
				}
				System.out.println("-----");
			}

			Long airportCount = airportCount = airportRepository.count().block();
			assertEquals(iatas.length, airportCount);

			airportCount = airportRepository.countByIataIn("JFK", "IAD", "SFO").block();
			assertEquals(3, airportCount);

			airportCount = airportRepository.countByIcaoAndIataIn("jfk", "JFK", "IAD", "SFO", "XXX").block();
			assertEquals(1, airportCount);

			airportCount = airportRepository.countByIataIn("XXX").block();
			assertEquals(0, airportCount);

		} finally {
			for (int i = 0; i < iatas.length; i++) {
				Airport airport = new Airport("airports::" + iatas[i], iatas[i] /*iata*/, iatas[i] /* lcao */);
				try {
					airportRepository.delete(airport).block();
				} catch (DataRetrievalFailureException drfe) {
					System.out.println("Failed to delete: " + airport);
				}
			}
		}
	}

	@Configuration
	@EnableReactiveCouchbaseRepositories("org.springframework.data.couchbase")
	static class Config extends AbstractCouchbaseConfiguration {

		@Override
		public String getConnectionString() {
			return connectionString();
		}

		@Override
		public String getUserName() {
			return config().adminUsername();
		}

		@Override
		public String getPassword() {
			return config().adminPassword();
		}

		@Override
		public String getBucketName() {
			return bucketName();
		}

	}

}
