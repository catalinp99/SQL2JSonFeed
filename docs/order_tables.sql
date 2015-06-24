
-- view
SELECT cpo.OrderNumber, cpo.OrderReference, cpo.AccountReference AS MerchantCode, cpo.PlacedDate, cpo.CancelDate, cpo.ConfirmDate, cpo.CpConfirmDate, cpo.CpPlacedDate, cpo.HubId, cpo.Status, cpoa.FirstName, cpoa.MiddleName, cpoa.LastName, COALESCE (cpoa.Street1, '') + ' || ' + COALESCE (cpoa.Street2, '') + ' || ' + COALESCE (cpoa.Street3, '') AS Street, cpoa.City, cpoa.Region, cpoa.Country, cpoa.PostalCode, 
         COALESCE (cpoa.PhoneNumber1, '') + ' || ' + COALESCE (cpoa.PhoneNumber2, '') AS PhoneNumber, cpoa.Email, cpodc.SKU, cpodc.URL, cpodc.OrderDetailsCommodityId, cpodc.Name, cpodc.HSCode, cpodl.Quantity, cpm.Name AS MerchantName, cpma.FirstName AS SellerFirstName, cpma.MiddleName AS SellerMiddleName, cpma.LastName AS SellerLastName, COALESCE (cpma.Street1, '') + ' || ' + COALESCE (cpma.Street2, '') 
         + ' || ' + COALESCE (cpma.Street3, '') AS SellerStreet, cpma.City AS SellerCity, cpma.Region AS SellerRegion, cpma.Country AS SellerCountry, cpma.PostalCode AS SellerPostalCode, COALESCE (cpma.PhoneNumber1, '') + ' || ' + COALESCE (cpma.PhoneNumber2, '') AS SellerPhoneNumber, cpma.Email AS SellerEmail, cpp.PartnerCode, partnerAddr.Company AS PartnerName, CASE WHEN (Cpodc.URL IS NOT NULL) AND (CHARINDEX(Cpodc.URL, 
         'www.ebay.com/itm') <> 0) THEN REVERSE(SUBSTRING(LTRIM(RTRIM(REVERSE(cpodc.URL))), 1, CHARINDEX('/', LTRIM(RTRIM(REVERSE(cpodc.URL))) - 1))) ELSE '' END AS EBAYITEMID
FROM  dbo.cpOrder AS cpo WITH (NOLOCK) INNER JOIN
         dbo.cpOrderDetails AS cpod WITH (NOLOCK) ON cpo.OrderDetailsId = cpod.OrderDetailsId INNER JOIN
         dbo.cpOrderConsignee AS cpoc WITH (NOLOCK) ON cpod.OrderConsigneeId = cpoc.OrderConsigneeId INNER JOIN
         dbo.cpOrderAddress AS cpoa WITH (NOLOCK) ON cpoc.AddressId = cpoa.AddressId INNER JOIN
         dbo.cpOrderDetailsLine AS cpodl WITH (NOLOCK) ON cpo.OrderDetailsId = cpodl.OrderDetailsId INNER JOIN
         dbo.cpOrderDetailsCommodity AS cpodc WITH (NOLOCK) ON cpodl.OrderDetailsCommodityId = cpodc.OrderDetailsCommodityId INNER JOIN
         dbo.cpMerchant AS cpm WITH (NOLOCK) ON cpo.AccountReference = cpm.MerchantCode AND cpm.PartnerCode = cpo.PartnerCode INNER JOIN
         dbo.cpMerchantAddress AS cpma WITH (NOLOCK) ON cpm.AddressId = cpma.AddressId INNER JOIN
         dbo.cpPartner AS cpp WITH (NOLOCK) ON cpm.PartnerCode = cpp.PartnerCode INNER JOIN
         dbo.cpPartnerAddress AS partnerAddr WITH (NOLOCK) ON cpp.AddressId = partnerAddr.AddressId

-- {"cpOrder", "cpOrderDetails", "cpOrderConsignee", "cpOrderAddress",
-- "cpOrderDetailsLine", "cpOrderDetailsCommodity",
-- "cpMerchant", "cpMerchantAddress", "cpPartner", "cpPartnerAddress",
-- "cpParcel", "cpInboundParcel", "cpExceptionParcel"}
		 
/*
cpOrder (order)
 (1) -> cpOrderDetails (order)
	(1) -> cpOrderConsignee (order)
		(1) -> cpOrderAddress (consigneeAddress)
--
	(m) -> cpOrderDetailsLine (orderLine)
	    (1) -> cpOrderDetailsCommodity (orderLine)
-- LATER: MUST BE A VIEW THROUGH cpMerchantCategory
		(0:1) -> cpMerchantCommodity () ON orderLine.SKU and merchant.merchantCode
			   (0:m) -> cpMerchantCommodityIdTypes 
--
 (1) -> cpMerchant (merchant)
	(1) -> cpMerchantAddress (merchantAddress)
	(1) -> cpPartner (partner)
		(1) -> cpPartnerAddress (partnerAddress)
--
 (0:m) -> cpParcel (parcel)
	(0:1) -> ParcelCentral.HubApp.LicensePlate
 (0:m) -> cpInboundParcel (inboundParcel)
 (0:m) -> cpExceptionParcel (exceptionParcel)
*/

--- The resulting
SELECT TOP (:limit) cpOrder.OrderNumber AS order___orderNumber
  , cpOrder.Status AS order___orderStatus
  , cpOrder.PlacedDate AS order___placedDate
  , cpOrder.ConfirmDate AS order___confirmDate
  , cpOrder.CpPlacedDate AS order___localPlacedDate
  , cpOrder.CpConfirmDate AS order___localConfirmDate
  , cpOrder.ExpiryDate AS order___expiryDate
  , cpOrder.CancelDate AS order___cancelDate
  , cpOrder.OrderReference AS order___orderReference
  , cpOrder.PartnerCode AS order___partnerCode
  , cpOrder.ShippingMethodId AS order___shippingMethodId
  , cpOrder.AccountReference AS order___merchantCode
  , cpOrder.HubId AS order___hubId
  , cpOrderConsignee.OrderConsigneeId AS order___consignee___orderConsigneeId
  , cpOrderConsignee.ConsigneeNumber AS order___consignee___consigneeNumber
  , cpOrderAddress.AddressId AS order___consignee___orderAddressId
  , cpOrderAddress.FirstName AS order___consignee___firstName
  , cpOrderAddress.LastName AS order___consignee___lastName
  , cpOrderAddress.middleName AS order___consignee___middleName
  , cpOrderAddress.FirstName + ' ' + COALESCE(cpOrderAddress.MiddleName, '') + ' ' + cpOrderAddress.LastName AS order___consignee___name
  , COALESCE(cpOrderAddress.Street1, '') + ' ' + COALESCE(cpOrderAddress.Street2, '') + ' ' + COALESCE(cpOrderAddress.Street3, '') AS order___consignee___street
  , cpOrderAddress.Company AS order___consignee___company
  , cpOrderAddress.Email AS order___consignee___email
  , cpOrderAddress.PhoneNumber1 AS order___consignee___phoneNumber1
  , cpOrderAddress.PhoneNumber2 AS order___consignee___phoneNumber2
  , cpOrderAddress.FaxNumber AS order___consignee___faxNumber
  , cpOrderAddress.PostalCode AS order___consignee___postalCode
  , cpOrderAddress.City AS order___consignee___city
  , cpOrderAddress.Region AS order___consignee___region
  , cpOrderAddress.Country AS order___consignee___countryCode
  , cpOrderAddress.BuyerId AS order___consignee___buyerId
  , cpOrderDetailsLine.OrderDetailsLineId AS order___orderLines___orderDetailsLineId
  , cpOrderDetailsLine.CountryOfOrigin AS order___orderLines___countryOfOriginCode
  , cpOrderDetailsLine.Quantity AS order___orderLines___quantity
  , cpOrderDetailsCommodity.OrderDetailsCommodityId AS order___orderLines___orderDetailsCommodityId
  , cpOrderDetailsCommodity.SKU AS order___orderLines___sku
  , cpOrderDetailsCommodity.Name AS order___orderLines___name
  , cpOrderDetailsCommodity.URL AS order___orderLines___url
  , cpOrderDetailsCommodity.HSCode AS order___orderLines___hsCode
  , cpOrderDetailsCommodity.CategoryPath AS order___orderLines___categoryPath
  , cpInboundParcel.InboundParcelNumber AS order___inboundParcels___inboundParcelNumber
  , cpInboundParcel.ParentInboundParcelNumber AS order___inboundParcels___parentInboundParcelNumber
  , cpInboundParcel.Status AS order___inboundParcels___status
  , cpInboundParcel.PlacedDate AS order___inboundParcels___placedDate
  , cpInboundParcel.CancellationDate AS order___inboundParcels___cancellationDate
  , cpInboundParcel.ProcessedDate AS order___inboundParcels___processedDate
  , cpInboundParcel.InboundCarrierID AS order___inboundParcels___inboundCarrierId
  , cpInboundParcel.InboundShippingMethodID AS order___inboundParcels___inboundShippingMethodId
  , cpInboundParcel.ParcelReference AS order___inboundParcels___parcelReference
  , cpInboundParcel.OrderNumber AS order___inboundParcels___orderNumber
  , cpInboundParcel.Identification AS order___inboundParcels___parcelIdentification
  , cpInboundParcelLine.InboundParcelNumber AS order___inboundParcels___inboundParcelLines___inboundParcelNumber
  , cpInboundParcelLine.InboundParcelLineId AS order___inboundParcels___inboundParcelLines___lineId
  , cpInboundParcelLine.CountryOfOrigin AS order___inboundParcels___inboundParcelLines___originCountryCode
  , cpInboundParcelLine.Quantity AS order___inboundParcels___inboundParcelLines___quantity
  , cpInboundParcelCommodity.InboundParcelCommodityId AS order___inboundParcels___inboundParcelLines___inboundParcelCommodityId
  , cpInboundParcelCommodity.SKU AS order___inboundParcels___inboundParcelLines___sku
  , cpInboundParcelCommodity.Name AS order___inboundParcels___inboundParcelLines___name
  , cpInboundParcelCommodity.URL AS order___inboundParcels___inboundParcelLines___url
  , cpInboundParcelCommodity.HSCode AS order___inboundParcels___inboundParcelLines___hsCode
  , cpInboundParcelCommodity.HSCodeSource AS order___inboundParcels___inboundParcelLines___hsCodeSource
  , cpInboundParcelCommodity.CategoryPath AS order___inboundParcels___inboundParcelLines___categoryPath
FROM cpOrder AS cpOrder
  INNER JOIN cpOrderDetails AS cpOrderDetails ON cpOrder.OrderDetailsId = cpOrderDetails.OrderDetailsId
  INNER JOIN cpOrderConsignee AS cpOrderConsignee ON cpOrderDetails.OrderConsigneeId = cpOrderConsignee.OrderConsigneeId
  INNER JOIN cpOrderAddress AS cpOrderAddress ON cpOrderConsignee.AddressId = cpOrderAddress.AddressId
  LEFT OUTER JOIN cpOrderDetailsLine AS cpOrderDetailsLine ON cpOrderDetails.OrderDetailsId = cpOrderDetailsLine.OrderDetailsId
  INNER JOIN cpOrderDetailsCommodity AS cpOrderDetailsCommodity ON cpOrderDetailsLine.OrderDetailsCommodityId = cpOrderDetailsCommodity.OrderDetailsCommodityId
  LEFT OUTER JOIN cpInboundParcel AS cpInboundParcel ON cpOrder.OrderNumber = cpInboundParcel.OrderNumber
  LEFT OUTER JOIN cpInboundParcelLine AS cpInboundParcelLine ON cpInboundParcel.InboundParcelNumber = cpInboundParcelLine.InboundParcelNumber
  LEFT OUTER JOIN cpInboundParcelCommodity AS cpInboundParcelCommodity ON cpInboundParcelLine.InboundParcelCommodityId = cpInboundParcelCommodity.InboundParcelCommodityId
WHERE cpOrder.CpPlacedDate > '2015-03-01 00:00:00.000'
      AND cpOrder.CpPlacedDate >= :last_ref_value
ORDER BY order___localPlacedDate ASC
  , order___orderNumber ASC
