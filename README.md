# idpay-recovery-error-topic
Application to handle the resubmit of error messages published into generic error topic until maxRetry occurs

## How test helm template

```sh
 helm dep build && helm template . -f values-dev.yaml  --debug
```

## How deploy helm

```sh
helm dep build && helm upgrade --namespace idpay --install --values values-dev.yaml --wait --timeout 5m0s idpay-recovery-error-topic .
```
