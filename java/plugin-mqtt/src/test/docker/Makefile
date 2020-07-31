test::
	docker-compose down
	docker-compose up -d
	docker-compose ps
	docker-compose logs -f test
	docker-compose down
test-shell::
	docker-compose down
	sed '/^[[:space:]]*command:/{s/^/#/};/command: .*bash\|tty: true/{s/^#//}' docker-compose.yml >docker-compose-shell.yml
	docker-compose -f docker-compose-shell.yml up -d
	rm docker-compose-shell.yml
	sleep 3  # XXX
	docker-compose ps
	docker exec -it docker_test_1 bash || true
	docker-compose down
