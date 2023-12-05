data "azurerm_subnet" "postgres" {
  name                 = "iaas"
  resource_group_name  = "ss-${var.env}-network-rg"
  virtual_network_name = "ss-${var.env}-vnet"
}

module "postgresql_flexible" {
  providers = {
    azurerm.postgres_network = azurerm.postgres_network
  }

  source               = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  env                  = var.env
  product              = var.product
  resource_group_name  = local.rg_name
  component            = var.component
  business_area        = "sds"
  location             = var.location
  pgsql_sku            = "GP_Standard_D2s_v3"
  pgsql_storage_mb     = 65536

  common_tags          = var.common_tags
  admin_user_object_id = var.jenkins_AAD_objectId
  pgsql_databases = [
    {
      name : local.db_name
    }
  ]

  pgsql_version = "15"
}

data "azurerm_key_vault" "key_vault" {
  name                = local.vault_name
  resource_group_name = local.rg_name
}

////////////////////////////////
// Populate Vault with DB info
////////////////////////////////

resource "azurerm_key_vault_secret" "POSTGRES-CONNECTION-STRING" {
  name         = "${var.component}-POSTGRES-CONNECTION-STRING"
  value        = "postgres://${module.postgresql_flexible.username}:${module.postgresql_flexible.password}@${module.postgresql_flexible.fqdn}:${local.db_port}/${local.db_name}"
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES-USER" {
  name         = "${var.component}-POSTGRES-USER"
  value        = module.postgresql_flexible.username
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES-PASS" {
  name         = "${var.component}-POSTGRES-PASS"
  value        = module.postgresql_flexible.password
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_HOST" {
  name         = "${var.component}-POSTGRES-HOST"
  value        = module.postgresql_flexible.fqdn
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_PORT" {
  name         = "${var.component}-POSTGRES-PORT"
  value        = local.db_port
  key_vault_id = data.azurerm_key_vault.key_vault.id
}

resource "azurerm_key_vault_secret" "POSTGRES_DATABASE" {
  name         = "${var.component}-POSTGRES-DATABASE"
  value        = local.db_name
  key_vault_id = data.azurerm_key_vault.key_vault.id
}