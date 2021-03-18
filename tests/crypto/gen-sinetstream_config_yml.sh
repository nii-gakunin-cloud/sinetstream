#!/bin/sh

password="sercret-000"

print1() {
cat <<EOF
${service}:
    crypto:
        ${algorithm:+algorithm: ${algorithm}}
        ${key_length:+key_length: ${key_length}}
        ${mode:+mode: ${mode}}
        ${padding:+padding: ${padding}}
        ${key_derivation:+key_derivation:
            ${kd_algorithm:+algorithm: ${kd_algorithm}}
            ${kd_salt_bytes:+salt_bytes: ${kd_salt_bytes}}
            ${kd_iteration:+iteration: ${kd_iteration}}
            ${kd_prf:+prf: ${kd_prf}}}
        password:
            value: ${password}
    topic: test-topic
    value_type: byte_array
    type: dummy
    brokers: broker.example.com
    data_encryption: true

EOF
}
print_service() {
    service="${algorithm}+${key_length}+${mode}+${padding}+${kd_algorithm}+${kd_salt_bytes}+${kd_iteration}+${kd_prf}"
    print1
}

validp() {
    # CBC requires padding.
    # GCM/EAX doesn't require padding.
    case "$mode-${padding:-none}" in
    CBC-none) return 1;;
    *-none) return 0;;
    GCM-*|EAX-*) return 1;;
    esac
    return 0
}

# test crypto (key_derivation is default values)
for algorithm in AES; do
 for key_length in "" 128 192 256; do
  for mode in CBC OFB CTR GCM EAX; do
   for padding in "" none pkcs7; do
    if ! validp; then
      continue
    fi
    key_derivation=""
    print_service
   done
  done
 done
done

# test key_derivation
for algorithm in AES; do
 for key_length in ""; do
  for mode in CBC GCM; do
   for padding in "" pkcs7; do
    if ! validp; then
      continue
    fi
    for kd_algorithm in "" pbkdf2; do
     for kd_salt_bytes in "" 8 12 16; do
      for kd_iteration in "" 1 1000 10000; do
       for kd_prf in "" HMAC-SHA256; do
        key_derivation="${kd_algorithm}${kd_salt_bytes}${kd_iteration}${kd_prf}"
        print_service
       done
      done
     done
    done
   done
  done
 done
done

exit
