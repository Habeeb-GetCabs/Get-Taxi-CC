package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.service.FloatingSpeedOverlayManager
import com.example.service.LocationTrackingService
import com.example.ui.components.CreatorSignatureBanner

/**
 * Consolidated About Us Screen for Taxi Meter Pro.
 *
 * Official business address:
 * Get Taxi
 * No. 286, Diwan Bahadur Road,
 * R.S. Puram, Coimbatore,
 * Tamil Nadu, India.
 *
 * Digital Solutions Division:
 * Habeeb Digital Solutions (HDS)
 * A unit of Get Taxi
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (!LocationTrackingService.isTripActivelyRunningForBubble()) {
            FloatingSpeedOverlayManager.forceHide(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "About Us",
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("about_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF475569)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFDFBFF))
            )
        },
        containerColor = Color(0xFFFDFBFF)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // HERO BRAND CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(28.dp))
                    .testTag("about_hero_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(2.dp, Color(0xFFFFC107), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.get_taxi_logo),
                            contentDescription = "GET TAXI Logo",
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "GET TAXI",
                        color = Color(0xFF0F172A),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Digital Solutions, Business Services & Taxi Software",
                        color = Color(0xFFB45309),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Version 1.0.0 • Pro Commercial Edition",
                            color = Color(0xFF475569),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // OFFICIAL BUSINESS ADDRESS & CONTACT CARD
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
                    .testTag("about_address_card")
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF14171C)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Address",
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "OFFICIAL BUSINESS ADDRESS",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "Get Taxi",
                                color = Color(0xFF0F172A),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "No. 286, Diwan Bahadur Road,\nR.S. Puram, Coimbatore,\nTamil Nadu, India.",
                        color = Color(0xFF1E293B),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(16.dp))

                    // CLICKABLE GET TAXI TELEGRAM GROUP LINK
                    Surface(
                        color = Color(0xFFF0F9FF),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                try {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://t.me/+dOFMD6gQOftlYTA1")
                                    )
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                            .testTag("about_telegram_link")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0284C7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "GET TAXI Telegram Group",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "GET TAXI Telegram Group",
                                        color = Color(0xFF0369A1),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "https://t.me/+dOFMD6gQOftlYTA1",
                                        color = Color(0xFF0F172A),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open Link",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // HABEEB DIGITAL SOLUTIONS (HDS) - A UNIT OF GET TAXI
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
                    .testTag("about_business_identity_card")
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = "Business",
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "DIGITAL SOLUTIONS DIVISION",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = "Habeeb Digital Solutions (HDS)",
                                color = Color(0xFF0F172A),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "A unit of Get Taxi",
                            color = Color(0xFF1E293B),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Habeeb Digital Solutions (HDS) delivers specialized enterprise digital products, high-performance web applications, commercial billing systems, and tailored taxi software.",
                        color = Color(0xFF475569),
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }

            // SERVICES OFFERED CARD (PRESERVED EXACTLY)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
                    .testTag("about_services_card")
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Text(
                        text = "SERVICES OFFERED",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val services = listOf(
                        ServiceItem(
                            title = "Website Design & Development",
                            description = "Custom responsive websites and business web portals engineered for speed, reliability, and mobile visitors.",
                            icon = Icons.Default.Language
                        ),
                        ServiceItem(
                            title = "Android App Development",
                            description = "Native Android applications built with Jetpack Compose, background location tracking, and clean offline-ready architecture.",
                            icon = Icons.Default.PhoneAndroid
                        ),
                        ServiceItem(
                            title = "Billing & Business Management Software",
                            description = "Commercial invoicing, fare/billing calculation, PDF receipt generation, and business operations management tools.",
                            icon = Icons.AutoMirrored.Filled.ReceiptLong
                        ),
                        ServiceItem(
                            title = "WhatsApp Automation",
                            description = "Automated customer follow-ups, booking notifications, status alerts, and structured WhatsApp workflows.",
                            icon = Icons.AutoMirrored.Filled.Send
                        ),
                        ServiceItem(
                            title = "AI Chatbots and Business Assistants",
                            description = "Intelligent conversational assistants and digital workflow bots for instant customer engagement and support.",
                            icon = Icons.Default.SmartToy
                        ),
                        ServiceItem(
                            title = "Google Ads Creation & Campaign Management",
                            description = "Targeted search and local ad campaigns, keyword strategy, and conversion tracking to grow business inquiries.",
                            icon = Icons.Default.Campaign
                        ),
                        ServiceItem(
                            title = "SEO Services",
                            description = "Technical and on-page search engine optimization built to improve organic search rankings and local visibility.",
                            icon = Icons.Default.Search
                        ),
                        ServiceItem(
                            title = "Google Business Profile Management",
                            description = "Google Maps listing optimization, service updates, and local presence management for customer discovery.",
                            icon = Icons.Default.Storefront
                        ),
                        ServiceItem(
                            title = "Custom Software Development",
                            description = "Bespoke desktop, mobile, and utility software tailored around real-world commercial workflows.",
                            icon = Icons.Default.Build
                        ),
                        ServiceItem(
                            title = "Business Process Automation",
                            description = "Streamlining repetitive operational tasks, reporting, and customer notifications with reliable digital systems.",
                            icon = Icons.Default.SettingsSuggest
                        ),
                        ServiceItem(
                            title = "Taxi and Transport Management Software",
                            description = "Specialized fleet dispatch tools, commercial tariff meters, driver settlement calculators, and trip billing systems.",
                            icon = Icons.Default.LocalTaxi
                        ),
                        ServiceItem(
                            title = "Landing Pages and Lead Generation",
                            description = "Focused promotional landing pages designed for high conversion rates and qualified lead capture.",
                            icon = Icons.Default.TrendingUp
                        )
                    )

                    services.forEachIndexed { index, service ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF14171C)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = service.icon,
                                    contentDescription = null,
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = service.title,
                                    color = Color(0xFF0F172A),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = service.description,
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        if (index < services.size - 1) {
                            HorizontalDivider(
                                color = Color(0xFFF8FAFC),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // PREMIUM CREDIT CARD AT THE VERY BOTTOM OF ABOUT US
            CreatorSignatureBanner(
                testTag = "about_creator_credit_banner"
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private data class ServiceItem(
    val title: String,
    val description: String,
    val icon: ImageVector
)
