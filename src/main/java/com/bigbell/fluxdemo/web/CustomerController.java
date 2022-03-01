package com.bigbell.fluxdemo.web;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bigbell.fluxdemo.domain.Customer;
import com.bigbell.fluxdemo.domain.CustomerRepository;
import com.bigbell.fluxdemo.domain.CustomerService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@RestController
public class CustomerController {

	private final CustomerRepository customerRepository; 
	private final Sinks.Many<Customer> sink;
	
	// A 요청 -> Flux -> Stream
	// B 요청 -> Flux -> Stream
	// -> Flux.merge -> sink
	
	public CustomerController(CustomerRepository customerRepository) {
		this.customerRepository = customerRepository;
		this.sink = Sinks.many().multicast().onBackpressureBuffer(); // unicast, multicast, replay
	}
	
	@GetMapping("/flux")
	public Flux<Integer> flux(){
		return Flux.just(1,2,3,4,5).delayElements(Duration.ofSeconds(1)).log();
	}
	
	@GetMapping(value="/fluxstream", produces=MediaType.APPLICATION_STREAM_JSON_VALUE)
	public Flux<Integer> fluxstream(){
		return Flux.just(1,2,3,4,5).delayElements(Duration.ofSeconds(1)).log();
	}
	
	@GetMapping(value="/customer", produces=MediaType.APPLICATION_STREAM_JSON_VALUE)
	public Flux<Customer> findAll() {
		return customerRepository.findAll().delayElements(Duration.ofSeconds(1)).log();
	}
	
	@GetMapping("/customer/{id}")
	public Mono<Customer> findById(@PathVariable Long id) {
		return customerRepository.findById(id).log();
	}
	
	@GetMapping(value="/customer/sse") // 생략 가능 produces=MediaType.TEXT_EVENT_STREAM_VALUE
	public Flux<ServerSentEvent<Customer>> findAllSSE() {
		return sink.asFlux().map(c->ServerSentEvent.builder(c).build()).doOnCancel(()->{
			sink.asFlux().blockLast();
		}) ;
	}
	
	@PostMapping("/customer")
	public Mono<Customer> save() {
		return customerRepository.save(new Customer("gildong", "Hong")).doOnNext(c->{
			sink.tryEmitNext(c);
		});
	}
}








